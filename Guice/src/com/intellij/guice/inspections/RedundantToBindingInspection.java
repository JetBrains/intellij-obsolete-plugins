// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.AnnotationUtils;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

public final class RedundantToBindingInspection extends BaseInspection {
  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("redundant.to.binding.problem.descriptor");
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
      if (!"to".equals(methodName)) {
        return;
      }
      final PsiExpression[] args = expression.getArgumentList().getExpressions();
      if (args.length != 1) {
        return;
      }
      final PsiExpression arg = args[0];
      if (!(arg instanceof PsiClassObjectAccessExpression)) {
        return;
      }
      final PsiTypeElement classTypeElement = ((PsiClassObjectAccessExpression)arg).getOperand();
      final PsiType classType = classTypeElement.getType();
      if (!(classType instanceof PsiClassType)) {
        return;
      }
      final PsiClass referentClass = ((PsiClassType)classType).resolve();
      if (referentClass == null) {
        return;
      }
      final PsiClass boundClass = GuiceUtils.findImplementedClassForBinding(expression);
      if (boundClass == null) {
        return;
      }
      if (GuiceUtils.findAnnotatedWithCallForBinding(expression) != null) {
        return;
      }
      if (AnnotationUtil.isAnnotated(boundClass, GuiceAnnotations.PROVIDED_BY, CHECK_HIERARCHY)) {
        return;
      }
      if (AnnotationUtil.isAnnotated(boundClass, GuiceAnnotations.IMPLEMENTED_BY, CHECK_HIERARCHY)) {
        final PsiAnnotation implementedByAnnotation = boundClass.getModifierList().findAnnotation(GuiceAnnotations.IMPLEMENTED_BY);
        if (implementedByAnnotation == null) return;
        final PsiElement defaultValue = AnnotationUtils.findDefaultValue(implementedByAnnotation);
        if (defaultValue == null) {
          return;
        }
        if (!(defaultValue instanceof PsiClassObjectAccessExpression)) {
          return;
        }
        final PsiTypeElement implementByClass = ((PsiClassObjectAccessExpression)defaultValue).getOperand();
        final PsiType implmenetedByClass = implementByClass.getType();
        if (!(implmenetedByClass instanceof PsiClassType)) {
          return;
        }
        final PsiClass implementedByClass = ((PsiClassType)implmenetedByClass).resolve();
        if (referentClass.equals(implementedByClass)) {
          registerError(classTypeElement);
        }
      }
      else {
        if (boundClass.equals(referentClass)) {
          registerError(classTypeElement);
        }
      }
    }
  }
}