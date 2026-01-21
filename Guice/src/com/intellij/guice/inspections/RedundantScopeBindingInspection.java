// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

public final class RedundantScopeBindingInspection extends BaseInspection {
  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("redundant.scope.binding.problem.descriptor");
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new Visitor();
  }

  @Override
  public @Nullable LocalQuickFix buildFix(PsiElement location, Object[] infos) {
    return new DeleteBindingFix();
  }

  private static class Visitor extends BaseInspectionVisitor {
    @Override
    public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
      super.visitMethodCallExpression(expression);
      final PsiReferenceExpression methodExpression = expression.getMethodExpression();
      final String methodName = methodExpression.getReferenceName();
      if (!"in".equals(methodName)) {
        return;
      }
      final PsiExpression[] args = expression.getArgumentList().getExpressions();
      if (args.length != 1) {
        return;
      }
      final PsiExpression arg = args[0];
      if (!(arg instanceof PsiReferenceExpression)) {
        return;
      }
      final String annotation = GuiceUtils.getScopeAnnotationForScopeExpression(arg);
      if (annotation == null) {
        return;
      }
      final PsiClass boundClass = GuiceUtils.findImplementedClassForBinding(expression);
      if (boundClass == null) {
        return;
      }
      if (!AnnotationUtil.isAnnotated(boundClass, annotation, CHECK_HIERARCHY)) {
        return;
      }
      registerError(arg);
    }
  }
}