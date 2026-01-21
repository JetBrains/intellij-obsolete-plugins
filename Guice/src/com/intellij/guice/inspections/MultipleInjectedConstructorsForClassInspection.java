// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

public final class MultipleInjectedConstructorsForClassInspection extends BaseInspection {
  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("multiple.injected.constructors.for.class.problem.descriptor");
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new Visitor();
  }

  private static class Visitor extends BaseInspectionVisitor {
    @Override
    public void visitMethod(@NotNull PsiMethod method) {
      super.visitMethod(method);
      if (!method.isConstructor()) {
        return;
      }
      final PsiClass containingClass = method.getContainingClass();
      if (containingClass == null) {
        return;
      }
      final PsiMethod[] constructors = containingClass.getConstructors();
      if (constructors.length <= 1) {
        return;
      }
      if (!AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
        return;
      }

      int annotatedConstructorCount = 0;
      for (PsiMethod constructor : constructors) {
        if (AnnotationUtil.isAnnotated(constructor, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
          annotatedConstructorCount++;
        }
      }
      if (annotatedConstructorCount > 1) {
        registerMethodError(method);
      }
    }
  }
}