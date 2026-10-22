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
 * before collecting navigation markers, and brings the index up to date for the
 * highlighted file first (see {@link GuiceProjectModel#ensureFileCurrent}).
 */
abstract class GuiceLineMarkerProviderBase extends RelatedItemLineMarkerProvider {

  @Override
  public final void collectNavigationMarkers(@NotNull List<? extends PsiElement> elements,
                                             @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                             boolean forNavigation) {
    if (elements.isEmpty()) return;
    PsiElement first = elements.getFirst();
    Module module = ModuleUtilCore.findModuleForPsiElement(first);
    // Test the module before the service exists: a project without Guice never creates the service.
    if (module == null || !GuiceProjectModel.isGuiceAvailable(module)) return;
    GuiceProjectModel model = GuiceProjectModel.getInstance(module.getProject());

    PsiFile file = first.getContainingFile();
    if (file != null) {
      model.ensureFileCurrent(file);
    }

    super.collectNavigationMarkers(elements, result, forNavigation);
  }
}
