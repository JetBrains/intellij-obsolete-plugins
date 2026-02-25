// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.html.impl.GspHtmlFileImpl;
import org.jetbrains.plugins.grails.references.common.GrailsLayoutFileReferenceSet;

public class GspLayoutReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    PsiFile htmlFile = element.getContainingFile();
    if (!(htmlFile instanceof GspHtmlFileImpl)) return PsiReference.EMPTY_ARRAY;

    return GrailsLayoutFileReferenceSet.createReferences(element);
  }
}
