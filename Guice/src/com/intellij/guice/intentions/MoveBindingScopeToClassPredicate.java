// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

public class MoveBindingScopeToClassPredicate implements PsiElementPredicate {
  @Override
  public boolean satisfiedBy(PsiElement element) {
    if (!GuiceUtils.isBinding(element)) {
      return false;
    }
    final PsiMethodCallExpression call = (PsiMethodCallExpression)element;
    if (GuiceUtils.findScopeForBinding(call) == null) {
      return false;
    }
    final PsiMethodCallExpression scopeCall = GuiceUtils.findScopeCallForBinding(call);
    final PsiExpression arg = scopeCall.getArgumentList().getExpressions()[0];
    final String scopeAnnotation = GuiceUtils.getScopeAnnotationForScopeExpression(arg);
    if (scopeAnnotation == null) {
      return false;
    }
    final PsiClass implementingClass = GuiceUtils.findImplementingClassForBinding(call);
    if (implementingClass == null) {
      return false;
    }
    if (AnnotationUtil.isAnnotated(implementingClass, "com.google.inject.Singleton", CHECK_HIERARCHY) ||
        AnnotationUtil.isAnnotated(implementingClass, "com.google.inject.servlet.RequestScoped", 0) ||
        AnnotationUtil.isAnnotated(implementingClass, "com.google.inject.servlet.SessionScoped", 0)) {
      return false;
    }
    return true;
  }
}
