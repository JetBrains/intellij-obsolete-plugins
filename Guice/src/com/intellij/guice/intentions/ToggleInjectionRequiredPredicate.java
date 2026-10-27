// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;

/**
 * Accepts a {@code com.google.inject.Inject} annotation. Only this annotation has the {@code optional}
 * attribute; {@code javax.inject.Inject} and {@code jakarta.inject.Inject} do not.
 */
public class ToggleInjectionRequiredPredicate implements PsiElementPredicate {
  @Override
  public boolean satisfiedBy(PsiElement element) {
    if (!(element instanceof PsiAnnotation annotation)) {
      return false;
    }
    return GuiceAnnotations.INJECT.equals(annotation.getQualifiedName());
  }
}
