// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.extensions.GuiceCallPattern;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;

/**
 * Reports pointless untargeted {@code bind()} calls where the bound class has no
 * {@code @Inject}-annotated constructors, fields, or methods, making the binding useless.
 */
public final class PointlessBindingInspection extends BaseUastInspection {
  public PointlessBindingInspection() {
    extendCall(
        GuiceCallPattern.named("bind")
            .requireValueOrTypeArgument()
            .statementOnly()
            .inGuicePackage()
            .withReceiverInheritor("com.google.inject.Module", "com.google.inject.Binder"),
        PointlessBindingInspection::checkBindCall
    );
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("pointless.binding.problem.descriptor");
  }

  @Override
  public @Nullable LocalQuickFix buildFix(PsiElement location, Object[] infos) {
    return new DeleteBindingFix(DeleteBindingFix.Mode.STATEMENT);
  }

  private static void checkBindCall(@NotNull UCallExpression expression, @NotNull BaseUastInspectionVisitor visitor) {
    PsiClass psiClass = GuiceUtils.resolveClassArgument(expression);
    if (psiClass == null || usesInject(psiClass)) {
      return;
    }
    visitor.registerError(expression);
  }

  private static boolean usesInject(PsiClass aClass) {
    for (PsiMethod method : aClass.getAllMethods()) {
      if (AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
        return true;
      }
    }
    for (PsiField field : aClass.getAllFields()) {
      if (AnnotationUtil.isAnnotated(field, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
        return true;
      }
    }
    return false;
  }
}
