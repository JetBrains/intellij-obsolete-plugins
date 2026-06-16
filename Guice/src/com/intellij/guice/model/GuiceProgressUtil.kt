// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model

import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportRawProgress
import java.util.function.Consumer

/**
 * Processes a set of files with a background progress indicator using the
 * modern coroutine-based progress API.
 *
 * - [withBackgroundProgress] shows a status bar progress with **built-in delayed visibility**
 *   (the UI is not shown immediately to avoid flickering for fast operations).
 * - [reportSequentialProgress] provides automatic per-file fraction updates.
 *
 * This function **blocks** the calling thread until all files are processed.
 * It must be called from a thread that has a current progress indicator or job
 * (e.g., a highlighting thread or inside `ProgressManager.runProcess`).
 *
 * @param project      the current project (for progress indicator scoping)
 * @param title        the progress bar title (e.g., "Building Guice model…")
 * @param files        the files to process
 * @param processFile  the callback to invoke for each file (runs in blocking context)
 */
fun processFilesWithProgressBlocking(
    project: Project,
    title: String,
    files: Collection<VirtualFile>,
    processFile: Consumer<VirtualFile>,
) {
    if (files.isEmpty()) return

    // Bridge from blocking thread → coroutine world.
    // runBlockingCancellable picks up the current thread's Job/indicator context,
    // making this operation cancellable from outside.
    runBlockingCancellable {
        withBackgroundProgress(project, title, cancellable = false) {
            val fileList = files.toList()
            val total = fileList.size
            reportRawProgress { reporter ->
                for ((index, file) in fileList.withIndex()) {
                    reporter.fraction(index.toDouble() / total)
                    reporter.details(file.name)
                    // Switch back to blocking context for PSI operations
                    // (which require a ProgressIndicator on the thread).
                    coroutineToIndicator {
                        processFile.accept(file)
                    }
                }
                reporter.fraction(1.0)
            }
        }
    }
}
