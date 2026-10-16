// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.uast.UCallExpression;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

public class MoveBindingToClassPredicate implements PsiElementPredicate {
  @Override
  public boolean satisfiedBy(PsiElement element) {
    final UCallExpression uCall = GuiceUtils.resolveOutermostBindingCall(element);
    if (uCall == null) return false;

    if (GuiceInjectionUtil.getCallExpressionType(uCall, "to") == null) {
      return false;
    }
    final PsiClass implementedClass = GuiceUtils.findImplementedClassForBinding(uCall);
    if (implementedClass == null) {
      return false;
    }
    if (AnnotationUtil.isAnnotated(implementedClass, GuiceAnnotations.IMPLEMENTED_BY, CHECK_HIERARCHY) ||
        AnnotationUtil.isAnnotated(implementedClass, GuiceAnnotations.PROVIDED_BY, 0)) {
      return false;
    }
    return true;
  }
}