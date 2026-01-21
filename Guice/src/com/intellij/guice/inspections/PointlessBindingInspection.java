// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PointlessBindingInspection extends BaseInspection {
  private static final Logger LOGGER = Logger.getInstance("PointlessBindingInspection");

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("pointless.binding.problem.descriptor");
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new Visitor();
  }

  @Override
  public @Nullable LocalQuickFix buildFix(PsiElement location, Object[] infos) {
    return new DeleteBindingFix();
  }

  private static class DeleteBindingFix implements LocalQuickFix {
    @Override
    public @NotNull String getName() {
      return GuiceBundle.message("delete.binding");
    }

    @Override
    public @NotNull String getFamilyName() {
      return "";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      final PsiMethodCallExpression element = (PsiMethodCallExpression)descriptor.getPsiElement();
      try {
        element.getParent().delete();
      }
      catch (IncorrectOperationException e) {
        LOGGER.error(e);
      }
    }
  }

  private static class Visitor extends BaseInspectionVisitor {
    @Override
    public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
      super.visitMethodCallExpression(expression);
      final PsiReferenceExpression methodExpression = expression.getMethodExpression();
      final String methodName = methodExpression.getReferenceName();
      if (!"bind".equals(methodName)) {
        return;
      }
      final PsiExpression[] args = expression.getArgumentList().getExpressions();
      if (args.length != 1) {
        return;
      }
      final PsiExpression arg = PsiUtil.skipParenthesizedExprDown(args[0]);
      if (!(arg instanceof PsiClassObjectAccessExpression)) {
        return;
      }
      final PsiTypeElement classTypeElement = ((PsiClassObjectAccessExpression)arg).getOperand();
      final PsiType classType = classTypeElement.getType();
      if (!(classType instanceof PsiClassType)) {
        return;
      }
      final PsiElement parent = expression.getParent();
      if (!(parent instanceof PsiExpressionStatement)) {
        return;
      }
      final PsiClass psiClass = ((PsiClassType)classType).resolve();
      if (psiClass != null && usesInject(psiClass)) {
        return;
      }
      registerError(expression);
    }

    private static boolean usesInject(PsiClass aClass) {
      for (PsiMethod method: aClass.getAllMethods()) {
        if (AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
          return true;
        }
      }
      for (PsiField field: aClass.getAllFields()) {
        if (AnnotationUtil.isAnnotated(field, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
          return true;
        }
      }
      return false;
    }
  }
}