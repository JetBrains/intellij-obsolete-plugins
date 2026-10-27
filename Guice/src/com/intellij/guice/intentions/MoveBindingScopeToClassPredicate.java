// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.uast.UCallExpression;

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
    final PsiExpression[] args = psiScopeCall.getArgumentList().getExpressions();
    if (args.length != 1) {
      return false;
    }
    final String scopeAnnotation = GuiceUtils.getScopeAnnotationForScopeExpression(args[0]);
    if (scopeAnnotation == null) {
      return false;
    }
    // The scope goes to the class that Guice constructs: the 'to' target, or the bound class itself
    // for an untargeted binding. Provider, instance and constructor bindings have no such class.
    final PsiClass scopedClass = GuiceUtils.findScopeAnnotatedClassForBinding(uCall);
    if (!MoveBindingToClassPredicate.isAnnotatableSourceClass(scopedClass)) {
      return false;
    }
    // Scope annotations are not inherited: only an annotation on the class itself conflicts.
    for (Collection<String> scopeGroup : GuiceAnnotations.SCOPE_GROUPS) {
      if (AnnotationUtil.isAnnotated(scopedClass, scopeGroup, 0)) {
        return false;
      }
    }
    return true;
  }
}
