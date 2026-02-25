// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.Map;

public final class ActionRenameProcessor extends RenamePsiElementProcessor {
  @Override
  public boolean canProcessElement(@NotNull PsiElement element) {
    return GrailsUtils.isControllerAction(element);
  }

  @Override
  public void prepareRenaming(@NotNull PsiElement element, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames) {
    PsiFile view = ContainerUtil.getFirstItem(GrailsUtils.getViewPsiByAction(element));
    if (view == null) return;

    String viewName = view.getName();
    int idx = viewName.lastIndexOf('.');
    String extension = idx == -1 ? "" : viewName.substring(idx);

    allRenames.put(view, newName + extension);
  }
}
