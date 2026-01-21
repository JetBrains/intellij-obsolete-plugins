// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

public class MoveBindingToClassPredicate implements PsiElementPredicate {
  @Override
  public boolean satisfiedBy(PsiElement element) {
    if (!GuiceUtils.isBinding(element)) {
      return false;
    }
    final PsiMethodCallExpression call = (PsiMethodCallExpression)element;

    if (GuiceUtils.findImplementingClassForBinding(call) == null) {
      return false;
    }
    final PsiClass implementedClass = GuiceUtils.findImplementedClassForBinding(call);
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