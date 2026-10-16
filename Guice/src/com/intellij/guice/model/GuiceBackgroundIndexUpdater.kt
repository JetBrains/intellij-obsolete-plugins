// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.guice.GuiceBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportRawProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Schedules background processing of Guice files and triggers
 * re-highlighting when the index is updated.
 *
 * Discovery and file extraction are broken into small, independent [smartReadAction] steps
 * so write actions can interleave without restarting completed steps from the beginning,
 * and each file's state is committed atomically only after extraction completes.
 */
class GuiceBackgroundIndexUpdater(
    private val project: Project,
    private val model: GuiceProjectModel,
    private val cs: CoroutineScope,
) {
    internal fun launchInScope(block: suspend CoroutineScope.() -> Unit): Job = cs.launch(block = block)

    companion object {
        private val LOG = logger<GuiceBackgroundIndexUpdater>()

        /** Debounce delay: wait this long after the last dirty event before processing. */
        private const val DEBOUNCE_MS = 300L
    }

    /** Timestamp (in System.nanoTime) of the most recent dirty-file notification. */
    @Volatile
    private var lastDirtyRequestNano: Long = 0L

    /** Current dirty-processing job. Drains dirty files in a loop without mid-update cancellation. */
    @Volatile
    private var dirtyJob: Job? = null

    /** Current population job. Loops until the latest structure generation is populated. */
    @Volatile
    private var populationJob: Job? = null

    /**
     * Schedules background processing of dirty files after a debounce delay.
     * Rapid events extend the debounce window without cancelling an already-running update pass.
     */
    fun scheduleDirtyProcessing() {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            // Tests process the dirty files in processDirtyFilesNow(), when they ask for the index.
            // Processing here runs inside the VFS event, and can see a file before its content is written.
            return
        }
        lastDirtyRequestNano = System.nanoTime()
        synchronized(this) {
            if (dirtyJob?.isActive == true) {
                return
            }
            dirtyJob = cs.launch {
                try {
                    runDirtyProcessingLoop()
                }
                finally {
                    synchronized(this@GuiceBackgroundIndexUpdater) {
                        dirtyJob = null
                    }
                }
            }
        }
    }

    private suspend fun runDirtyProcessingLoop() {
        while (true) {
            while (true) {
                val elapsedMs = (System.nanoTime() - lastDirtyRequestNano) / 1_000_000
                val remainingMs = DEBOUNCE_MS - elapsedMs
                if (remainingMs <= 0) break
                delay(remainingMs)
            }

            if (!model.hasDirtyFiles()) {
                synchronized(this) {
                    if (!model.hasDirtyFiles()) return
                }
                continue
            }

            val anyProcessed = withBackgroundProgress(
                project,
                GuiceBundle.message("progress.updating.guice.model"),
                TaskCancellation.nonCancellable(),
            ) {
                // reportRawProgress MUST be the first thing inside withBackgroundProgress
                // to take ownership of the progress step.
                reportRawProgress { reporter ->
                    var updated = false
                    while (true) {
                        val files = model.collectDirtyFiles()
                        if (files.isEmpty()) break
                        val total = files.size
                        for ((index, vf) in files.withIndex()) {
                            reporter.fraction(index.toDouble() / total)
                            reporter.details(vf.name)
                            val changed = smartReadAction(project) {
                                processSafely(vf) { model.processSingleDirtyFile(vf) }
                            }
                            if (changed) updated = true
                        }
                    }
                    reporter.fraction(1.0)
                    updated
                }
            }

            if (anyProcessed) {
                DaemonCodeAnalyzer.getInstance(project).restart("Guice model was updated")
            }

            synchronized(this) {
                if (!model.hasDirtyFiles()) return
            }
        }
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
    }

    /**
     * Schedules initial population (or structure-change re-population) of the index in the background.
     *
     * Discovery and per-file extraction run in separate [smartReadAction] steps so write actions
     * can interleave without restarting completed work from the beginning.
     */
    fun scheduleInitialPopulation(module: Module) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            try {
                runReadAction {
                    populateSynchronously(module)
                }
            }
            finally {
                model.onPopulationFinished()
            }
            return
        }
        synchronized(this) {
            if (populationJob?.isActive == true) {
                model.onPopulationFinished()
                return
            }
            populationJob = cs.launch {
                try {
                    runPopulationLoop(module)
                }
                finally {
                    synchronized(this@GuiceBackgroundIndexUpdater) {
                        populationJob = null
                    }
                    model.onPopulationFinished()
                }
            }
        }
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
        model.completePopulationIfCurrent(generation, pathsBefore, discoveredFiles)
    }

    private suspend fun runPopulationLoop(module: Module) {
        var anyCompleted = false
        while (model.isPopulationNeeded) {
            val generation = model.structureGeneration
            val pathsBefore = model.indexedFilesSnapshot

            val completed = withBackgroundProgress(
                project,
                GuiceBundle.message("progress.building.guice.model"),
                TaskCancellation.nonCancellable(),
            ) {
                // reportRawProgress MUST be first to take ownership of the progress step.
                reportRawProgress { reporter ->
                    val steps = smartReadAction(project) { model.createDiscoverySteps(module) }
                    val discoveredFiles = LinkedHashSet<VirtualFile>()
                    for (step in steps) {
                        if (generation != model.structureGeneration) return@reportRawProgress false
                        val stepFiles = smartReadAction(project) { step.discover() }
                        discoveredFiles.addAll(stepFiles)
                    }

                    val fileList = ArrayList(discoveredFiles)
                    val total = fileList.size
                    for ((index, vf) in fileList.withIndex()) {
                        if (generation != model.structureGeneration) return@reportRawProgress false
                        reporter.fraction(index.toDouble() / total)
                        reporter.details(vf.name)
                        smartReadAction(project) {
                            processSafely(vf) { model.processDiscoveredFile(vf, generation) }
                        }
                    }

                    while (true) {
                        if (generation != model.structureGeneration) return@reportRawProgress false
                        val dirtyBatch = model.collectDirtyFiles()
                        if (dirtyBatch.isEmpty()) break
                        for (vf in dirtyBatch) {
                            if (generation != model.structureGeneration) return@reportRawProgress false
                            smartReadAction(project) {
                                processSafely(vf) { model.processSingleDirtyFile(vf) }
                            }
                        }
                    }

                    reporter.fraction(1.0)
                    model.completePopulationIfCurrent(generation, pathsBefore, discoveredFiles)
                }
            }

            if (completed) {
                anyCompleted = true
            }
        }

        if (anyCompleted) {
            DaemonCodeAnalyzer.getInstance(project).restart("Guice modules were refreshed")
        }
    }

    /**
     * Runs the action for one file. An exception in one file must not stop the processing of the other files.
     * Control-flow exceptions, such as cancellation, still go up.
     */
    private inline fun processSafely(vf: VirtualFile, action: () -> Boolean): Boolean {
        try {
            return action()
        }
        catch (e: Exception) {
            if (e is ControlFlowException || e is CancellationException) throw e
            LOG.warn("Cannot extract Guice entries from ${vf.path}", e)
            return false
        }
    }
}
