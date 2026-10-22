// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.uast.UCallExpression;

public class MoveBindingToClassPredicate implements PsiElementPredicate {
  @Override
  public boolean satisfiedBy(PsiElement element) {
    final UCallExpression uCall = GuiceUtils.resolveOutermostBindingCall(element);
    if (uCall == null) return false;

    if (GuiceInjectionUtil.getCallExpressionType(uCall, "to") == null) {
      return false;
    }
    final PsiClass implementedClass = GuiceUtils.findImplementedClassForBinding(uCall);
    if (!isAnnotatableSourceClass(implementedClass)) {
      return false;
    }
    // JIT annotations are not inherited: only an annotation on the class itself conflicts.
    if (AnnotationUtil.isAnnotated(implementedClass, GuiceAnnotations.IMPLEMENTED_BY, 0) ||
        AnnotationUtil.isAnnotated(implementedClass, GuiceAnnotations.PROVIDED_BY, 0)) {
      return false;
    }
    return true;
  }

  /**
   * Returns {@code true} when the intention can add an annotation to {@code psiClass}: the class is
   * a project source class with a modifier list, not a library class.
   */
  static boolean isAnnotatableSourceClass(PsiClass psiClass) {
    return psiClass != null
           && psiClass.getModifierList() != null
           && psiClass.getManager().isInProject(psiClass)
           && psiClass.isWritable();
  }
}
