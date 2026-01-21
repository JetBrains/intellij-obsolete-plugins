// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;


public final class GuiceImplicitUsageProvider implements ImplicitUsageProvider {
  private static final Collection<String> GUICE_INJECTION_POINT = Arrays.asList(GuiceAnnotations.INJECT, GuiceAnnotations.JAVAX_INJECT, GuiceAnnotations.JAKARTA_INJECT);

  @Override
  public boolean isImplicitUsage(@NotNull PsiElement element) {
    return isImplicitRead(element);
  }

  @Override
  public boolean isImplicitRead(@NotNull PsiElement element) {
    return element instanceof PsiMethod && AnnotationUtil.isAnnotated((PsiModifierListOwner)element, GUICE_INJECTION_POINT, 0) ||
           element instanceof PsiModifierListOwner && AnnotationUtil.isAnnotated((PsiModifierListOwner)element, GuiceAnnotations.PROVIDES, 0);
  }

  @Override
  public boolean isImplicitWrite(@NotNull PsiElement element) {
    return element instanceof PsiModifierListOwner && AnnotationUtil.isAnnotated((PsiModifierListOwner)element, GUICE_INJECTION_POINT, 0);
  }
}