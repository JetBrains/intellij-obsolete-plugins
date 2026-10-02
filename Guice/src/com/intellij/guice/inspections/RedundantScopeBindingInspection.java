// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.model.extensions.GuiceCallPattern;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

import java.util.Collection;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

/**
 * Reports redundant {@code .in()} scope bindings where the bound class already declares
 * the same scope via an annotation (e.g., {@code @Singleton}, {@code @RequestScoped}).
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
  public @Nullable LocalQuickFix buildFix(PsiElement location, Object[] infos) {
    return new DeleteBindingFix(DeleteBindingFix.Mode.CHAIN_CALL);
  }

  private static void checkInCall(@NotNull UCallExpression expression, @NotNull BaseUastInspectionVisitor visitor) {
    final UExpression arg = expression.getValueArguments().getFirst();
    final Collection<String> scopeAnnotations = GuiceUtils.getScopeAnnotationsForScopeExpression(arg);
    if (scopeAnnotations == null || scopeAnnotations.isEmpty()) {
      return;
    }
    final PsiClass boundClass = GuiceUtils.findImplementedClassForBinding(expression);
    if (boundClass == null) {
      return;
    }
    if (!AnnotationUtil.isAnnotated(boundClass, scopeAnnotations, CHECK_HIERARCHY)) {
      return;
    }
    visitor.registerError(arg);
  }
}
