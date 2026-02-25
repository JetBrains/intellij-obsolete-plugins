// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GrailsLayoutFileReferenceSet;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

public class ControllerLayoutReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    PsiClass containingClass = PsiUtil.getContainingNotInnerClass(element);

    if (!GrailsArtifact.CONTROLLER.isInstance(containingClass)) return PsiReference.EMPTY_ARRAY;
    assert containingClass != null;

    return GrailsLayoutFileReferenceSet.createReferences(element);
  }
}
