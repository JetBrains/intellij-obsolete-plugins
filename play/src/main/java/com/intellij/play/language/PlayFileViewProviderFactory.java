/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language;

import com.intellij.lang.Language;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.FileViewProviderFactory;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

public class PlayFileViewProviderFactory implements FileViewProviderFactory{
  @Override
  @NotNull
  public FileViewProvider createFileViewProvider(@NotNull final VirtualFile file, final Language language, @NotNull final PsiManager manager, final boolean eventSystemEnabled) {
    return new PlayFileViewProvider(manager, file, eventSystemEnabled);
  }
}
