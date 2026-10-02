// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

import java.util.Collection;

public class MoveBindingScopeToClassPredicate implements PsiElementPredicate {
  @Override
  public boolean satisfiedBy(PsiElement element) {
    final UCallExpression uCall = GuiceUtils.resolveOutermostBindingCall(element);
    if (uCall == null) return false;

    final UCallExpression scopeCall = GuiceUtils.findCallInChain(uCall, "in");
    if (scopeCall == null) {
      return false;
    }
    final PsiElement scopeCallPsi = scopeCall.getSourcePsi();
    if (!(scopeCallPsi instanceof PsiMethodCallExpression psiScopeCall)) {
      return false;
    }
    final PsiExpression arg = psiScopeCall.getArgumentList().getExpressions()[0];
    final String scopeAnnotation = GuiceUtils.getScopeAnnotationForScopeExpression(arg);
    if (scopeAnnotation == null) {
      return false;
    }
    final PsiClass implementingClass = GuiceInjectionUtil.getCallExpressionType(uCall, "to");
    if (implementingClass == null) {
      return false;
    }
    for (Collection<String> scopeGroup : GuiceAnnotations.SCOPE_GROUPS) {
      if (AnnotationUtil.isAnnotated(implementingClass, scopeGroup, CHECK_HIERARCHY)) {
        return false;
      }
    }
    return true;
  }
}
