// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.extensions.GuiceCallPattern;
import com.intellij.guice.utils.AnnotationUtils;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

/**
 * Reports redundant {@code .toProvider()} bindings where the provider class is the same as
 * the one already declared via {@code @ProvidedBy} on the bound type.
 */
public final class RedundantToProviderBindingInspection extends BaseUastInspection {
  public RedundantToProviderBindingInspection() {
    extendCall(
        GuiceCallPattern.named("toProvider").requireValueOrTypeArgument(),
        RedundantToProviderBindingInspection::checkToProviderCall
    );
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("redundant.to.provider.binding.problem.descriptor");
  }

  @Override
  public @Nullable LocalQuickFix buildFix(PsiElement location, Object[] infos) {
    return new DeleteBindingFix(DeleteBindingFix.Mode.CHAIN_CALL);
  }

  private static void checkToProviderCall(@NotNull UCallExpression expression, @NotNull BaseUastInspectionVisitor visitor) {
    PsiClass referentClass = GuiceUtils.resolveClassArgument(expression);
    if (referentClass == null) {
      return;
    }
    final PsiClass boundClass = GuiceUtils.findImplementedClassForBinding(expression);
    if (boundClass == null) {
      return;
    }
    if (!AnnotationUtil.isAnnotated(boundClass, GuiceAnnotations.PROVIDED_BY, CHECK_HIERARCHY)) {
      return;
    }
    final PsiClass providedByClass =
      AnnotationUtils.resolveAnnotationClassValue(boundClass, GuiceAnnotations.PROVIDED_BY);
    if (referentClass.equals(providedByClass)) {
      visitor.registerClassArgumentError(expression);
    }
  }
}
