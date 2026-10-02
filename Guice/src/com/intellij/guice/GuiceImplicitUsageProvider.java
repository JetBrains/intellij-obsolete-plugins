// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.extensions.GuiceExtensionIndex;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;


public final class GuiceImplicitUsageProvider implements ImplicitUsageProvider {
  @Override
  public boolean isImplicitUsage(@NotNull PsiElement element) {
    return isImplicitRead(element);
  }

  /**
   * Guice calls an {@code @Inject} method or constructor and a {@code @Provides} method, so these count as read.
   * Guice only writes an {@code @Inject} field. The code must still read the field, else the field is unused.
   */
  @Override
  public boolean isImplicitRead(@NotNull PsiElement element) {
    if (!(element instanceof PsiModifierListOwner owner)) return false;
    GuiceExtensionIndex extensionIndex = GuiceExtensionIndex.get();
    if (owner instanceof PsiField) {
      return !extensionIndex.getSupportedFieldAnnotations().isEmpty()
          && AnnotationUtil.isAnnotated(owner, extensionIndex.getSupportedFieldAnnotations(), 0);
    }
    if (owner instanceof PsiMethod && AnnotationUtil.isAnnotated(owner, GuiceAnnotations.INJECTS, 0)) return true;
    return AnnotationUtil.isAnnotated(owner, extensionIndex.getAllProvidesAnnotations(), 0)
        || (!extensionIndex.getMethodAnnotations().isEmpty()
            && AnnotationUtil.isAnnotated(owner, extensionIndex.getMethodAnnotations(), 0));
  }

  @Override
  public boolean isImplicitWrite(@NotNull PsiElement element) {
    return element instanceof PsiModifierListOwner && AnnotationUtil.isAnnotated((PsiModifierListOwner)element, GuiceAnnotations.INJECTS, 0);
  }
}