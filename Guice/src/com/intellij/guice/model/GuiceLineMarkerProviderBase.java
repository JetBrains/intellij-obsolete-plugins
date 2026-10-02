// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Shared base for Guice line marker providers that checks module availability
 * and re-indexes the current file inline before collecting markers.
 */
abstract class GuiceLineMarkerProviderBase extends RelatedItemLineMarkerProvider {

  @Override
  public final void collectNavigationMarkers(@NotNull List<? extends PsiElement> elements,
                                             @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                             boolean forNavigation) {
    if (elements.isEmpty()) return;
    Module module = ModuleUtilCore.findModuleForPsiElement(elements.getFirst());
    if (module == null) return;
    GuiceProjectModel model = GuiceProjectModel.getInstance(module.getProject());
    if (!model.isGuiceAvailable(module)) return;

    // Re-index the current file inline (cancellable) for immediate feedback.
    // This ensures the file the user is editing has fresh entries in the
    // navigation index, without waiting for the background debounced processing.
    PsiFile psiFile = elements.getFirst().getContainingFile();
    if (psiFile != null) {
      model.reindexCurrentFile(psiFile);
    }

    super.collectNavigationMarkers(elements, result, forNavigation);
  }
}
