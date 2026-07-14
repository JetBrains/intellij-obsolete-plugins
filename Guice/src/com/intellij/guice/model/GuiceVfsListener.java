// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A {@link BulkFileListener} that reacts to VFS events and marks relevant files as dirty
 * in the {@link GuiceProjectModel}, triggering incremental recomputation of Guice bindings.
 *
 * <p>Only Java and Kotlin source files are considered relevant.  Directory events and files
 * with other extensions are silently ignored.
 *
 * <h3>Registration</h3>
 * Register in {@code plugin.xml} under {@code <projectListeners>}:
 * <pre>{@code
 *   <listener class="com.intellij.guice.model.GuiceVfsListener"
 *             topic="com.intellij.openapi.vfs.newvfs.BulkFileListener"/>
 * }</pre>
 * The platform injects the {@link Project} via the constructor.
 *
 * <h3>Event handling</h3>
 * <ul>
 *   <li>{@link VFileDeleteEvent} → {@link GuiceProjectModel#removeFile(VirtualFile)}</li>
 *   <li>{@link VFileContentChangeEvent}, {@link VFileCreateEvent}, {@link VFileCopyEvent},
 *       {@link VFileMoveEvent} → {@link GuiceProjectModel#markFileDirty(VirtualFile)}</li>
 *   <li>{@link VFilePropertyChangeEvent} (name change) →
 *       {@link GuiceProjectModel#markFileDirty(VirtualFile)}</li>
 * </ul>
 *
 * @see GuiceProjectModel
 */
public final class GuiceVfsListener implements BulkFileListener {

  private final Project myProject;

  /**
   * Constructor called by the platform when using {@code <projectListeners>} in {@code plugin.xml}.
   *
   * @param project the project this listener is associated with
   */
  public GuiceVfsListener(@NotNull Project project) {
    myProject = project;
  }

  @Override
  public void after(@NotNull List<? extends VFileEvent> events) {
    if (myProject.isDisposed()) return;

    GuiceProjectModel model = GuiceProjectModel.getInstance(myProject);

    for (VFileEvent event : events) {
      VirtualFile file = event.getFile();
      if (file == null || !isRelevantFile(file)) continue;

      if (event instanceof VFileDeleteEvent) {
        model.removeFile(file);
      } else if (event instanceof VFileContentChangeEvent ||
                 event instanceof VFileCreateEvent ||
                 event instanceof VFileCopyEvent ||
                 event instanceof VFileMoveEvent) {
        model.markFileDirty(file);
      } else if (event instanceof VFilePropertyChangeEvent propEvent) {
        if (VirtualFile.PROP_NAME.equals(propEvent.getPropertyName())) {
          model.markFileDirty(file);
        }
      }
    }
  }

  /**
   * Determines whether a VFS file is relevant for Guice binding analysis.
   * Only Java ({@code .java}) and Kotlin ({@code .kt}) source files are considered relevant.
   *
   * @param file the file to check
   * @return {@code true} if the file should be tracked for Guice binding changes
   */
  private static boolean isRelevantFile(@NotNull VirtualFile file) {
    if (file.isDirectory()) return false;
    String ext = file.getExtension();
    return "java".equals(ext) || "kt".equals(ext);
  }
}
