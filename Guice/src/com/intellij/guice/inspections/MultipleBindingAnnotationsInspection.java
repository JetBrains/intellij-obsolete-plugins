// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.GuiceBundle;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

public final class MultipleBindingAnnotationsInspection extends BaseInspection {
  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("multiple.binding.annotations.problem.descriptor");
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new Visitor();
  }

  private static class Visitor extends BaseInspectionVisitor {
    @Override
    public void visitVariable(@NotNull PsiVariable variable) {
      super.visitVariable(variable);
      final PsiModifierList modifiers = variable.getModifierList();
      if (modifiers == null) {
        return;
      }
      final PsiAnnotation[] annotations = modifiers.getAnnotations();
      int numBindingAnnotations = 0;
      for (PsiAnnotation annotation : annotations) {
        if (isBindingAnnotation(annotation)) {
          numBindingAnnotations++;
        }
      }
      if (numBindingAnnotations > 1) {
        registerVariableError(variable);
      }
    }
  }

  public static boolean isBindingAnnotation(PsiAnnotation annotation) {
    final PsiJavaCodeReferenceElement referenceElement = annotation.getNameReferenceElement();
    if (referenceElement == null) {
      return false;
    }
    final PsiElement element = referenceElement.resolve();
    if (!(element instanceof PsiClass annotationClass)) {
      return false;
    }
    return AnnotationUtil.isAnnotated(annotationClass, "com.google.inject.BindingAnnotation", CHECK_HIERARCHY);
  }
}