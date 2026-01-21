// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public final class UninstantiableBindingInspection extends BaseInspection {

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("uninstantiable.binding.problem.descriptor");
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new Visitor();
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
      if (GuiceUtils.isInstantiable(referentClass)) {
        return;
      }
      final PsiMethod method = expression.resolveMethod();
      if (method == null) {
        return;
      }
      final PsiClass containingClass = method.getContainingClass();
      if (!"com.google.inject.binder.LinkedBindingBuilder".equals(containingClass.getQualifiedName())) {
        return;
      }
      PsiClass moduleClass = PsiTreeUtil.getParentOfType(expression, PsiClass.class);
      if (moduleClass != null && GuiceUtils.provides(moduleClass, referentClass)) {
        return;
      }
      registerError(classTypeElement);
    }
  }
}