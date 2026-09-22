// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.guice.GuiceBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.RawProgressReporter
import com.intellij.platform.util.progress.reportRawProgress
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiFile
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
    companion object {
        /** Debounce delay: wait this long after the last dirty event before processing. */
        private const val DEBOUNCE_MS = 300L
    }

    /** Current debounced dirty-processing job. Cancelled and replaced on each new event. */
    private var dirtyJob: Job? = null

    /** Current population job. Only one runs at a time; duplicates are ignored. */
    private var populationJob: Job? = null

    /**
     * Schedules background processing of dirty files after a debounce delay.
     * Multiple rapid calls are coalesced — only the last one triggers processing.
     */
    fun scheduleDirtyProcessing() {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            runReadActionBlocking {
                val snapshot = GuiceInjectorManager.ContributorSnapshot.create()
                val files = model.collectDirtyFiles()
                if (files.isNotEmpty()) {
                    for (vf in files) {
                        model.processSingleDirtyFile(vf, snapshot)
                    }
                }
            }
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
                    val snapshot = GuiceInjectorManager.ContributorSnapshot.create()
                    val files = smartReadAction(project) { model.collectDirtyFiles() }
                    if (files.isEmpty()) return@reportRawProgress false

                    processFiles(reporter, files) { vf ->
                        model.processSingleDirtyFile(vf, snapshot)
                    }
                    true
                }
            }

            if (anyProcessed) {
                DaemonCodeAnalyzer.getInstance(project).restart()
            }
        }
    }

    /**
     * Schedules initial population of the index in the background.
     * Shows a progress bar in the status bar with per-file fraction and file name.
     *
     * Each file is processed inside its own [smartReadAction] so we release
     * the read lock between files, allowing write actions to proceed.
     *
     * @param module the module whose scope defines what files to include
     */
    fun scheduleInitialPopulation(module: Module) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
           runReadActionBlocking {
              model.clearIndices()
              val files = model.discoverRelevantFiles(module)
              if (files.isNotEmpty()) {
                  val snapshot = GuiceInjectorManager.ContributorSnapshot.create()
                  for (vf in files) {
                      model.processFile(vf, snapshot)
                  }
              }
              model.markPopulationComplete()
           }
            return
        }
        if (populationJob?.isActive == true) return
        populationJob = cs.launch {
            withBackgroundProgress(
                project,
                GuiceBundle.message("progress.building.guice.model"),
                TaskCancellation.nonCancellable(),
            ) {
                model.clearIndices()

                // reportRawProgress MUST be first to take ownership of the progress step.
                reportRawProgress { reporter ->
                    val files = smartReadAction(project) { model.discoverRelevantFiles(module) }
                    if (files.isEmpty()) {
                        model.markPopulationComplete()
                        return@reportRawProgress
                    }

                    val snapshot = GuiceInjectorManager.ContributorSnapshot.create()

                    processFiles(reporter, files) { vf ->
                        model.processFile(vf, snapshot)
                    }

                    model.markPopulationComplete()
                }
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
            smartReadAction(project) { processOne(vf) }
        }
        reporter.fraction(1.0)
    }
}

/**
 * Re-indexes a single PSI file into the navigation index.
 *
 * This is designed to be called from the highlighting thread (cancellable).
 * It extracts [GuiceEntry] instances from the file's top-level classes
 * and updates the [GuiceNavigationIndex] for that file.
 *
 * @param file the PSI file to re-index
 * @param navigationIndex the navigation index to update
 */
fun reindexFileInline(file: PsiFile, navigationIndex: GuiceNavigationIndex) {
    val vf = file.virtualFile ?: return
    val entries = mutableSetOf<GuiceEntry>()

    if (file is PsiClassOwner) {
        for (cls: PsiClass in file.classes) {
            entries.addAll(GuiceEntryProducer.extractFromClass(cls))
        }
    }
    navigationIndex.updateFile(vf.path, entries)
}
