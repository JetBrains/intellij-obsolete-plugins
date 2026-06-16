// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

import java.util.List;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

public final class RedundantScopeBindingInspection extends BaseUastInspection {
  public RedundantScopeBindingInspection() {
    super(UCallExpression.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("redundant.scope.binding.problem.descriptor");
  }

  @Override
  public @NotNull AbstractUastNonRecursiveVisitor buildUastVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new Visitor(this, holder, isOnTheFly);
  }

  @Override
  public @Nullable LocalQuickFix buildFix(PsiElement location, Object[] infos) {
    return new DeleteBindingFix();
  }

  private static class Visitor extends BaseUastInspectionVisitor {
    Visitor(@NotNull BaseUastInspection inspection, @NotNull ProblemsHolder holder, boolean onTheFly) {
      super(inspection, holder, onTheFly);
    }

    @Override
    public boolean visitCallExpression(@NotNull UCallExpression expression) {
      final String methodName = expression.getMethodName();
      if (!"in".equals(methodName)) {
        return true;
      }
      final List<UExpression> args = expression.getValueArguments();
      if (args.size() != 1) {
        return true;
      }
      final UExpression arg = args.get(0);
      final PsiElement argPsi = arg.getSourcePsi();
      if (argPsi == null) {
        return true;
      }
      if (!(argPsi instanceof com.intellij.psi.PsiExpression)) {
        return true;
      }
      final String annotation = GuiceUtils.getScopeAnnotationForScopeExpression((com.intellij.psi.PsiExpression)argPsi);
      if (annotation == null) {
        return true;
      }
      final PsiClass boundClass = GuiceUtils.findImplementedClassForBinding(expression);
      if (boundClass == null) {
        return true;
      }
      if (!AnnotationUtil.isAnnotated(boundClass, annotation, CHECK_HIERARCHY)) {
        return true;
      }
      registerError(arg);
      return true;
    }
  }
}