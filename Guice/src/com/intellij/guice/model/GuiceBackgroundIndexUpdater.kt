// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model

import com.intellij.guice.GuiceBundle
import com.intellij.guice.model.extensions.GuiceExtensionIndex
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportRawProgress
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

/**
 * Schedules background processing of Guice files and triggers
 * re-highlighting when the index changes.
 *
 * Requests arrive through two conflated flows. [dirtyRequests] is debounced: a drain of the dirty
 * files starts [DEBOUNCE_MS] after the last request, and a request that arrives during a drain
 * starts the next drain after the current one. [populationRequests] runs one population loop at
 * a time; a request that arrives during a loop runs a second loop, which exits at once when the
 * first loop already populated the latest structure generation.
 *
 * Discovery and file extraction are broken into small, independent [smartReadAction] steps
 * so write actions can interleave without restarting completed steps from the beginning,
 * and each file's state is committed atomically only after extraction completes.
 *
 * In unit-test mode the updater is synchronous: [scheduleInitialPopulation] populates at once,
 * and [scheduleDirtyProcessing] is a no-op because [GuiceProjectModel.getNavigationIndex] drains
 * the dirty files through [processDirtyFilesNow]. A test can switch to the background mode
 * with [enableBackgroundModeForTests].
 */
@OptIn(FlowPreview::class)
class GuiceBackgroundIndexUpdater(
    private val project: Project,
    private val model: GuiceProjectModel,
    private val cs: CoroutineScope,
) {
    companion object {
        private val LOG = logger<GuiceBackgroundIndexUpdater>()

        /**
         * Tells if an exception from one pass must end the collector coroutine. Only the cancellation of the
         * collector itself does. A [ProcessCanceledException] (which extends [CancellationException]) or a
         * cancellation of an inner scope belongs to the pass; any other failure is logged.
         *
         * @param coroutineActive whether the collector coroutine is still active after the exception
         */
        @JvmStatic
        @ApiStatus.Internal
        fun isFatalForCollector(e: Throwable, coroutineActive: Boolean): Boolean = !coroutineActive

        /** Debounce delay: wait this long after the last dirty event before processing. */
        private val DEBOUNCE = 300L.milliseconds
    }

    @Volatile
    private var synchronous: Boolean = ApplicationManager.getApplication().isUnitTestMode

    private val collectorsStarted = AtomicBoolean()

    /** The collector coroutines, kept so that a test can stop them with its scope. */
    private val collectorJobs = mutableListOf<Job>()

    private val dirtyRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val populationRequests = MutableSharedFlow<Module>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        if (!synchronous) {
            startCollectors()
        }
        subscribeToWorkspaceModel()
    }

    /**
     * Marks the structure changed when a library, module, content root or source root changes, so the next
     * index request rebuilds the index with fresh PSI references. The subscription lives in the service, so a
     * project without Guice (where nothing creates the service) pays nothing.
     *
     * [WorkspaceModel.eventLog][com.intellij.platform.backend.workspace.WorkspaceModel.eventLog] replays the
     * last change to a new subscriber. That change is already part of the state the service starts from, so
     * an event whose result is the snapshot taken at subscription time is skipped.
     */
    private fun subscribeToWorkspaceModel() {
        val workspaceModel = project.workspaceModel
        val snapshotAtSubscription = workspaceModel.currentSnapshot
        cs.launch {
            workspaceModel.eventLog.collect { event ->
                if (event.storageAfter === snapshotAtSubscription) return@collect
                val structureChanged = event.getChanges(LibraryEntity::class.java).isNotEmpty() ||
                                       event.getChanges(ModuleEntity::class.java).isNotEmpty() ||
                                       event.getChanges(ContentRootEntity::class.java).isNotEmpty() ||
                                       event.getChanges(SourceRootEntity::class.java).isNotEmpty()
                if (structureChanged) {
                    model.markStructureChanged()
                }
            }
        }
    }

    /**
     * Whether the updater processes requests on the calling thread. True only in unit-test mode.
     */
    fun isSynchronous(): Boolean = synchronous

    /**
     * Switches a unit test to the background mode of production until [parentDisposable] is disposed.
     */
    @TestOnly
    @JvmOverloads
    fun enableBackgroundModeForTests(parentDisposable: Disposable, startDirtyCollector: Boolean = true) {
        synchronous = false
        startCollectors(startDirtyCollector)
        Disposer.register(parentDisposable, {
            synchronous = true
            stopCollectors()
        })
    }

    /** Whether collector coroutines are running. */
    @TestOnly
    fun collectorsRunningForTests(): Boolean = synchronized(collectorJobs) { collectorJobs.any { it.isActive } }

    private fun stopCollectors() {
        val jobs = synchronized(collectorJobs) {
            val copy = collectorJobs.toList()
            collectorJobs.clear()
            copy
        }
        jobs.forEach { it.cancel() }
        collectorsStarted.set(false)
    }

    private fun startCollectors(startDirtyCollector: Boolean = true) {
        if (!collectorsStarted.compareAndSet(false, true)) return
        val jobs = mutableListOf<Job>()
        if (startDirtyCollector) {
            jobs += cs.launch {
                dirtyRequests.debounce(DEBOUNCE).collect {
                    runGuarded("dirty files") { processDirtyFilesInBackground() }
                }
            }
        }
        jobs += cs.launch {
            populationRequests.collect { module ->
                runGuarded("population") { runPopulationLoop(module) }
            }
        }
        synchronized(collectorJobs) { collectorJobs += jobs }
    }

    /**
     * Keeps a collector alive when one pass fails with an unexpected exception.
     */
    private suspend fun runGuarded(what: String, block: suspend () -> Unit) {
        try {
            block()
        }
        catch (e: Throwable) {
            if (isFatalForCollector(e, currentCoroutineContext().isActive)) throw e
            if (e is CancellationException) {
                // A cancellation of one pass, for example AlreadyDisposedException from a module that went away,
                // is not a cancellation of the collector. The next request runs a new pass.
                // A cancellation exception must not reach the logger, so only its kind is noted.
                LOG.debug { "Guice $what update was cancelled (${e.javaClass.simpleName})" }
            }
            else {
                LOG.error("Guice $what update failed", e)
            }
        }
    }

    /**
     * Requests background processing of dirty files after a debounce delay.
     * Rapid requests extend the debounce window without cancelling an already-running drain.
     */
    fun scheduleDirtyProcessing() {
        if (synchronous) {
            // Tests process the dirty files in processDirtyFilesNow(), when they ask for the index.
            return
        }
        dirtyRequests.tryEmit(Unit)
    }

    private suspend fun processDirtyFilesInBackground() {
        if (!model.hasDirtyFiles()) {
            model.requestRestartOfAffectedEditors(GuiceProjectModel.RestartMode.IMMEDIATELY, null)
            return
        }

        withBackgroundProgress(
            project,
            GuiceBundle.message("progress.updating.guice.model"),
            TaskCancellation.nonCancellable(),
        ) {
            // reportRawProgress MUST be the first thing inside withBackgroundProgress
            // to take ownership of the progress step.
            reportRawProgress { reporter ->
                while (true) {
                    val files = model.collectDirtyFiles()
                    if (files.isEmpty()) break
                    val total = files.size
                    for ((index, vf) in files.withIndex()) {
                        reporter.fraction(index.toDouble() / total)
                        reporter.details(vf.name)
                        smartReadAction(project) {
                            processSafely(vf) { model.processSingleDirtyFile(vf) }
                        }
                    }
                }
                reporter.fraction(1.0)
            }
        }

        model.requestRestartOfAffectedEditors(GuiceProjectModel.RestartMode.IMMEDIATELY, null)
    }

    /**
     * Processes all pending dirty files (and any dependent registrars they enqueue) synchronously.
     * Only tests use it, through [GuiceProjectModel.getNavigationIndex].
     * Must be called under a read action.
     */
    fun processDirtyFilesNow() {
        while (true) {
            val files = model.collectDirtyFiles()
            if (files.isEmpty()) break
            for (vf in files) {
                processSafely(vf) { model.processSingleDirtyFile(vf) }
            }
        }
        // Runs inside a highlighting pass, see GuiceProjectModel.getNavigationIndex.
        model.requestRestartOfAffectedEditors(GuiceProjectModel.RestartMode.WHEN_DAEMON_FINISHES, null)
    }

    /**
     * Requests initial population (or structure-change re-population) of the index in the background.
     * Repeated requests are conflated.
     *
     * Discovery and per-file extraction run in separate [smartReadAction] steps so write actions
     * can interleave without restarting completed work from the beginning.
     */
    fun scheduleInitialPopulation(module: Module) {
        if (synchronous) {
            runReadAction {
                populateSynchronously(module)
            }
            return
        }
        populationRequests.tryEmit(module)
    }

    private fun populateSynchronously(module: Module) {
        val generation = model.structureGeneration
        val pathsBefore = model.indexedFilesSnapshot
        val discoveredFiles = LinkedHashSet<VirtualFile>()
        for (step in model.createDiscoverySteps(module)) {
            discoveredFiles.addAll(step.discover())
        }
        for (vf in discoveredFiles) {
            processSafely(vf) { model.processDiscoveredFile(vf, generation) }
        }
        processDirtyFilesNow()
        runReadAction { model.completePopulationIfCurrent(generation, pathsBefore, discoveredFiles) }
        model.requestRestartOfAffectedEditors(GuiceProjectModel.RestartMode.WHEN_DAEMON_FINISHES, null)
    }

    private suspend fun runPopulationLoop(module: Module) {
        while (model.isPopulationNeeded) {
            if (module.isDisposed) return

            val generation = model.structureGeneration
            val pathsBefore = model.indexedFilesSnapshot

            withBackgroundProgress(
                project,
                GuiceBundle.message("progress.building.guice.model"),
                TaskCancellation.nonCancellable(),
            ) {
                // reportRawProgress MUST be first to take ownership of the progress step.
                reportRawProgress { reporter ->
                    val steps = smartReadAction(project) { model.createDiscoverySteps(module) }
                    val discoveredFiles = LinkedHashSet<VirtualFile>()
                    for (step in steps) {
                        if (generation != model.structureGeneration) return@reportRawProgress
                        val stepFiles = smartReadAction(project) { step.discover() }
                        discoveredFiles.addAll(stepFiles)
                    }

                    val fileList = ArrayList(discoveredFiles)
                    val total = fileList.size
                    for ((index, vf) in fileList.withIndex()) {
                        if (generation != model.structureGeneration) return@reportRawProgress
                        reporter.fraction(index.toDouble() / total)
                        reporter.details(vf.name)
                        smartReadAction(project) {
                            processSafely(vf) { model.processDiscoveredFile(vf, generation) }
                        }
                    }

                    while (true) {
                        if (generation != model.structureGeneration) return@reportRawProgress
                        val dirtyBatch = model.collectDirtyFiles()
                        if (dirtyBatch.isEmpty()) break
                        for (vf in dirtyBatch) {
                            if (generation != model.structureGeneration) return@reportRawProgress
                            smartReadAction(project) {
                                processSafely(vf) { model.processSingleDirtyFile(vf) }
                            }
                        }
                    }

                    reporter.fraction(1.0)
                    // The prune compares entries, and entry equality takes a read action (smart pointers).
                    // Take the read action first, then the index lock, like every other caller of the index.
                    val outcome = readAction { model.completePopulationIfCurrent(generation, pathsBefore, discoveredFiles) }
                    if (outcome.completed) {
                        // Files without entries of their own (an interface whose class gutter shows bindings of
                        // other files) are not in the affected set: after a population, every open file is restarted.
                        model.restartAllOpenFilesAfterPopulation()
                    }
                }
            }
        }

        model.requestRestartOfAffectedEditors(GuiceProjectModel.RestartMode.IMMEDIATELY, null)
    }

    /**
     * Runs the action for one file. A failure in one file, also an [Error] such as a [LinkageError] from
     * a contributor with a missing dependency, must not stop the processing of the other files.
     * Control-flow exceptions, such as cancellation, still go up (see [GuiceExtensionIndex.rethrowIfControlFlow]).
     */
    private inline fun processSafely(vf: VirtualFile, action: () -> Boolean): Boolean {
        try {
            return action()
        }
        catch (e: Throwable) {
            GuiceExtensionIndex.rethrowIfControlFlow(e)
            LOG.warn("Cannot extract Guice entries from ${vf.path}", e)
            return false
        }
    }
}
