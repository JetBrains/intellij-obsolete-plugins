// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.guice.model.GuiceQualifiers;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

import java.util.List;

public class MoveBindingToClassPredicate implements PsiElementPredicate {
  @Override
  public boolean satisfiedBy(PsiElement element) {
    final UCallExpression uCall = GuiceUtils.resolveOutermostBindingCall(element);
    if (uCall == null) return false;

    if (GuiceInjectionUtil.getCallExpressionType(uCall, "to") == null) {
      return false;
    }
    // @ImplementedBy applies to the unqualified key of the class itself: a qualified key, a multibinder element
    // or a link to another key (Key.get) cannot move to the class without changing what Guice injects.
    if (!isPlainLinkedBinding(uCall, "to")) {
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
   * Tells if {@code chain} is a standard {@code bind(X.class).<targetCall>(Y.class)} chain of an unqualified key
   * whose target is a class, not a {@code Key.get(...)} link.
   */
  static boolean isPlainLinkedBinding(@NotNull UCallExpression chain, @NotNull String targetCall) {
    if (!GuiceUtils.isStandardBindChain(chain) || GuiceUtils.isQualifiedBindChain(chain)) return false;
    UCallExpression target = GuiceUtils.findCallInChain(chain, targetCall);
    if (target == null) return false;
    List<UExpression> args = target.getValueArguments();
    return args.size() != 1 || GuiceQualifiers.asKeyGet(args.getFirst()) == null;
  }

  /**
   * Returns {@code true} when the intention can add an annotation to {@code psiClass}: the class is
   * a project source class with a modifier list, not a library class.
   */
  static boolean isAnnotatableSourceClass(PsiClass psiClass) {
    return psiClass != null
           && psiClass.getModifierList() != null
           && psiClass.getManager().isInProject(psiClass)
           && psiClass.isWritable()
           // The intentions edit Java PSI; a Kotlin class is a light class whose modifier list rejects the edit.
           && psiClass.getContainingFile() instanceof PsiJavaFile
           && !(psiClass instanceof PsiCompiledElement);
  }
}
