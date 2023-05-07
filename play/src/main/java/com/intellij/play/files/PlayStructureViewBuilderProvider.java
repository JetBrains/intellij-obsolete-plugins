/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.files;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.impl.TemplateLanguageStructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayStructureViewBuilderProvider implements PsiStructureViewFactory{
  @Override
  @Nullable
  public StructureViewBuilder getStructureViewBuilder(@NotNull final PsiFile psiFile) {
    return TemplateLanguageStructureViewBuilder.create(psiFile, PlayStructureViewModel::new);
  }
}
