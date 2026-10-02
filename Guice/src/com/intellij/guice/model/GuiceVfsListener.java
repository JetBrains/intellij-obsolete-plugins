// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A {@link BulkFileListener} that reacts to VFS events and marks relevant files as dirty
 * in the {@link GuiceProjectModel}, triggering incremental recomputation of Guice bindings.
 *
 * <h3>Registration</h3>
 * Register in {@code plugin.xml} under {@code <projectListeners>}:
 * <pre>{@code
 *   <listener class="com.intellij.guice.model.GuiceVfsListener"
 *             topic="com.intellij.openapi.vfs.newvfs.BulkFileListener"/>
 * }</pre>
 * The platform injects the {@link Project} via the constructor.
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
  public void before(@NotNull List<? extends VFileEvent> events) {
    if (myProject.isDisposed()) return;

    GuiceProjectModel model = GuiceProjectModel.getInstance(myProject);

    for (VFileEvent event : events) {
      VirtualFile file = event.getFile();
      if (file == null || (!file.isDirectory() && !isRelevantFile(file))) continue;

      if (event instanceof VFileDeleteEvent || event instanceof VFileMoveEvent) {
        model.removeFile(file);
      }
      else if (event instanceof VFilePropertyChangeEvent propEvent
               && VirtualFile.PROP_NAME.equals(propEvent.getPropertyName())) {
        model.removeFile(file);
      }
    }
  }

  @Override
  public void after(@NotNull List<? extends VFileEvent> events) {
    if (myProject.isDisposed()) return;

    GuiceProjectModel model = GuiceProjectModel.getInstance(myProject);
    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(myProject);

    for (VFileEvent event : events) {
      if (event instanceof VFileDeleteEvent) {
        VirtualFile file = event.getFile();
        if (file.isDirectory() || isRelevantFile(file)) {
          model.removeFile(file);
        }
      } else if (event instanceof VFileCopyEvent copyEvent) {
        VirtualFile created = copyEvent.findCreatedFile();
        if (created != null) {
          markDirtyRecursively(created, fileIndex, model);
        }
      } else if (event instanceof VFileContentChangeEvent ||
                 event instanceof VFileCreateEvent ||
                 event instanceof VFileMoveEvent) {
        VirtualFile file = event.getFile();
        if (file != null) {
          markDirtyRecursively(file, fileIndex, model);
        }
      } else if (event instanceof VFilePropertyChangeEvent propEvent
                 && VirtualFile.PROP_NAME.equals(propEvent.getPropertyName())) {
        VirtualFile file = event.getFile();
        markDirtyRecursively(file, fileIndex, model);
      }
    }
  }

  private static void markDirtyRecursively(@NotNull VirtualFile fileOrDir,
                                           @NotNull ProjectFileIndex fileIndex,
                                           @NotNull GuiceProjectModel model) {
    if (!fileOrDir.isValid() || !fileIndex.isInContent(fileOrDir)) return;
    if (fileOrDir.isDirectory()) {
      VfsUtilCore.visitChildrenRecursively(fileOrDir, new VirtualFileVisitor<Void>() {
        @Override
        public boolean visitFile(@NotNull VirtualFile child) {
          if (!fileIndex.isInContent(child)) return false;
          if (isRelevantFile(child)) {
            model.markFileDirty(child);
          }
          return true;
        }
      });
    } else if (isRelevantFile(fileOrDir)) {
      model.markFileDirty(fileOrDir);
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
