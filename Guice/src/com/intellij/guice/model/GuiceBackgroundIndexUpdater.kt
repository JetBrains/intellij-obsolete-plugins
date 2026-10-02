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
import com.intellij.platform.util.progress.RawProgressReporter
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
 * Uses [withBackgroundProgress] with [TaskCancellation.nonCancellable]
 * so that index population is never interrupted by highlighting cancellation.
 * Each file is processed inside its own [smartReadAction] so we release
 * the read lock between files, allowing write actions to proceed,
 * and wait for smart mode (index ready) before PSI access.
 *
 * Debouncing of dirty-file processing is done via coroutine [delay].
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

    /** Current debounced dirty-processing job. Cancelled and replaced on each new event. */
    @Volatile
    private var dirtyJob: Job? = null

    /** Current population job. Only one runs at a time; duplicates are ignored. */
    @Volatile
    private var populationJob: Job? = null

    /**
     * Schedules background processing of dirty files after a debounce delay.
     * Multiple rapid calls are coalesced — only the last one triggers processing.
     */
    fun scheduleDirtyProcessing() {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            // Tests process the dirty files in processDirtyFilesNow(), when they ask for the index.
            // Processing here runs inside the VFS event, and can see a file before its content is written.
            return
        }
        dirtyJob?.cancel()
        dirtyJob = cs.launch {
            delay(DEBOUNCE_MS)
            val anyProcessed = withBackgroundProgress(
                project,
                GuiceBundle.message("progress.updating.guice.model"),
                TaskCancellation.nonCancellable(),
            ) {
                // reportRawProgress MUST be the first thing inside withBackgroundProgress
                // to take ownership of the progress step. If smartReadAction or anything
                // else runs first, it consumes the step and reportRawProgress falls back
                // to EmptyRawProgressReporter.
                reportRawProgress { reporter ->
                    val files = model.collectDirtyFiles()
                    if (files.isEmpty()) return@reportRawProgress false

                    processFiles(reporter, files) { vf ->
                        model.processSingleDirtyFile(vf)
                    }
                    true
                }
            }

            if (anyProcessed) {
                DaemonCodeAnalyzer.getInstance(project).restart("Guice model was updated")
            }
        }
    }

    /**
     * Processes the dirty files synchronously. Only tests use it, through [GuiceProjectModel.getNavigationIndex].
     * Must be called under a read action.
     */
    fun processDirtyFilesNow() {
        val files = model.collectDirtyFiles()
        if (files.isEmpty()) return
        for (vf in files) {
            processSafely(vf) { model.processSingleDirtyFile(vf) }
        }
    }

    /**
     * Schedules initial population of the index in the background.
     * Shows a progress bar in the status bar with per-file fraction and file name.
     *
     * Each file is processed inside its own [smartReadAction] so we release
     * the read lock between files, allowing write actions to proceed.
     *
     * The population updates the index in place and prunes stale files at the end,
     * so the existing gutter icons stay visible while it runs.
     *
     * @param module the module whose scope defines what files to include
     * @param generation the structure generation when the population was requested
     */
    fun scheduleInitialPopulation(module: Module, generation: Int) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            try {
                runReadAction {
                    val pathsBefore = model.getIndexedFilesSnapshot()
                    val files = model.discoverRelevantFiles(module)
                    for (vf in files) {
                        processSafely(vf) { model.processFile(vf) }
                    }
                    model.pruneFilesExcept(pathsBefore, files)
                    model.markPopulationComplete(generation)
                }
            }
            finally {
                model.onPopulationFinished()
            }
            return
        }
        if (populationJob?.isActive == true) {
            model.onPopulationFinished()
            return
        }
        populationJob = cs.launch {
            try {
                withBackgroundProgress(
                    project,
                    GuiceBundle.message("progress.building.guice.model"),
                    TaskCancellation.nonCancellable(),
                ) {
                    // reportRawProgress MUST be first to take ownership of the progress step.
                    reportRawProgress { reporter ->
                        val pathsBefore = model.getIndexedFilesSnapshot()
                        val files = smartReadAction(project) { model.discoverRelevantFiles(module) }

                        processFiles(reporter, files) { vf ->
                            model.processFile(vf)
                        }

                        model.pruneFilesExcept(pathsBefore, files)
                        model.markPopulationComplete(generation)
                    }
                }
            }
            finally {
                model.onPopulationFinished()
            }

            DaemonCodeAnalyzer.getInstance(project).restart("Guice modules were refreshed")
        }
    }

    /**
     * Shared per-file processing loop with progress reporting.
     *
     * Reports per-file fraction and file name details via the [reporter].
     * Each file is processed inside its own [smartReadAction] so write actions
     * can interleave between files.
     *
     * @param reporter    the raw progress reporter (from [reportRawProgress])
     * @param files       the files to process
     * @param processOne  the action to perform on each file (called under smart read action)
     */
    private suspend fun processFiles(
        reporter: RawProgressReporter,
        files: Collection<VirtualFile>,
        processOne: (VirtualFile) -> Unit,
    ) {
        val fileList = ArrayList(files)
        val total = fileList.size

        for ((index, vf) in fileList.withIndex()) {
            reporter.fraction(index.toDouble() / total)
            reporter.details(vf.name)
            smartReadAction(project) { processSafely(vf) { processOne(vf) } }
        }
        reporter.fraction(1.0)
    }

    /**
     * Runs the action for one file. An exception in one file must not stop the processing of the other files.
     * Control-flow exceptions, such as cancellation, still go up.
     */
    private inline fun processSafely(vf: VirtualFile, action: () -> Unit) {
        try {
            action()
        }
        catch (e: Exception) {
            if (e is ControlFlowException || e is CancellationException) throw e
            LOG.warn("Cannot extract Guice entries from ${vf.path}", e)
        }
    }
}
