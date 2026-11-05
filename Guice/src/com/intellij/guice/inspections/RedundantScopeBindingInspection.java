// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.model.extensions.GuiceCallPattern;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

import java.util.Collection;


/**
 * Reports a redundant {@code .in()} scope binding: the class that Guice constructs for the binding
 * already declares the same scope with an annotation (such as {@code @Singleton} or {@code @RequestScoped}).
 *
 * <p>That class is the {@code to()} class of a linked binding or the bound class of an untargeted binding,
 * see {@link GuiceUtils#findScopeAnnotatedClassForBinding}. Scope annotations are not inherited.
 */
public final class RedundantScopeBindingInspection extends BaseUastInspection {
  public RedundantScopeBindingInspection() {
    extendCall(
        GuiceCallPattern.named("in").argumentCount(1),
        RedundantScopeBindingInspection::checkInCall
    );
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("redundant.scope.binding.problem.descriptor");
  }

  @Override
  public LocalQuickFix buildFix() {
    return new DeleteBindingFix(DeleteBindingFix.Mode.CHAIN_CALL, "remove.scope.binding");
  }

  private static void checkInCall(@NotNull UCallExpression expression, @NotNull BaseUastInspectionVisitor visitor) {
    final UExpression arg = expression.getValueArguments().getFirst();
    final Collection<String> scopeAnnotations = GuiceUtils.getScopeAnnotationsForScopeExpression(arg);
    if (scopeAnnotations == null || scopeAnnotations.isEmpty()) {
      return;
    }
    if (!GuiceUtils.isStandardBindChain(expression)) {
      return;
    }
    final PsiClass scopedClass = GuiceUtils.findScopeAnnotatedClassForBinding(expression);
    if (scopedClass == null) {
      return;
    }
    if (!AnnotationUtil.isAnnotated(scopedClass, scopeAnnotations, 0)) {
      return;
    }
    visitor.registerError(arg);
  }
}
