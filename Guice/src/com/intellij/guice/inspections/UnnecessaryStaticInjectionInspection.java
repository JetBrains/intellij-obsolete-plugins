// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class UnnecessaryStaticInjectionInspection extends BaseInspection {
  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("unnecessary.static.injection.problem.descriptor");
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
      if (!"requestStaticInjection".equals(methodName)) {
        return;
      }
      final PsiExpression[] args = expression.getArgumentList().getExpressions();
      for (PsiExpression arg : args) {
        if (!(arg instanceof PsiClassObjectAccessExpression)) {
          continue;
        }
        final PsiTypeElement classTypeElement = ((PsiClassObjectAccessExpression)arg).getOperand();
        final PsiType classType = classTypeElement.getType();
        if (!(classType instanceof PsiClassType)) {
          continue;
        }
        final PsiClass classToBindStatically = ((PsiClassType)classType).resolve();
        if (classToBindStatically == null) {
          continue;
        }
        if (!classHasStaticInjects(classToBindStatically)) {
          registerError(classTypeElement);
        }
      }
    }

    private static boolean classHasStaticInjects(PsiClass aClass) {
      final PsiMethod[] methods = aClass.getMethods();
      for (PsiMethod method : methods) {
        if (method.hasModifierProperty(PsiModifier.STATIC) &&
            AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
          return true;
        }
      }
      final PsiField[] fields = aClass.getFields();
      for (PsiField field : fields) {
        if (field.hasModifierProperty(PsiModifier.STATIC) &&
            AnnotationUtil.isAnnotated(field, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
          return true;
        }
      }
      return false;
    }
  }
}