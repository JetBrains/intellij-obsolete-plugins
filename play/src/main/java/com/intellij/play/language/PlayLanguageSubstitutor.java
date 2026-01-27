/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class PlayLanguageSubstitutor extends LanguageSubstitutor {
  @Override
  public Language getLanguage(@NotNull final VirtualFile file, @NotNull final Project project) {
    if (file instanceof VirtualFileWindow) return null;
    
    // Avoid calling isPlayInstalled() here to prevent circular dependency during indexing
    // File path structure (views directory) is sufficient to identify Play projects
    if (FileTypeRegistry.getInstance().isFileOfType(file, StdFileTypes.HTML) && checkViewsParent(file)) {
      return PlayLanguage.INSTANCE;
    }

    if ("tag".equals(file.getExtension())) {
      final VirtualFile tagsDir = file.getParent();
      if (checkDirName(tagsDir, "tags") && checkDirName(tagsDir.getParent(), "views")) {
        return PlayLanguage.INSTANCE;
      }
    }
    return null;
  }

  private static boolean checkDirName(@Nullable VirtualFile tagsDir, @NotNull String dirName) {
    return tagsDir != null && dirName.equals(tagsDir.getName());
  }

  private static boolean checkViewsParent(@NotNull VirtualFile file) {
    VirtualFile parent = file.getParent();

    while (parent != null) {
      if (parent.isDirectory() && "views".equals(parent.getName())) return true;
      parent = parent.getParent();
    }
    return false;
  }
}
