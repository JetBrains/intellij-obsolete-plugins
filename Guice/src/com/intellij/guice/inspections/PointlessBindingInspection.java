// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

/**
 * Reports pointless untargeted {@code bind()} calls where the bound class has no
 * {@code @Inject}-annotated constructors, fields, or methods, making the binding useless.
 *
 * <p>Example:
 * <pre>
 * // Flagged: Foo has no @Inject constructor, fields, or methods
 * bind(Foo.class);
 * bind&lt;Foo&gt;()          // Kotlin
 *
 * // OK: Foo has @Inject members
 * bind(Bar.class);     // where Bar has @Inject constructor
 *
 * // OK: targeted binding (has .to(...) chain)
 * bind(Foo.class).to(FooImpl.class);
 * </pre>
 */
public final class PointlessBindingInspection extends BaseUastInspection {
  public PointlessBindingInspection() {
    super(UCallExpression.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("pointless.binding.problem.descriptor");
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
      if (!"bind".equals(expression.getMethodName())) {
        return true;
      }
      PsiClass psiClass = GuiceUtils.resolveClassArgument(expression);
      if (psiClass == null) {
        return true;
      }
      // Check that this is an untargeted binding (i.e. not part of a chain like bind(...).to(...))
      if (GuiceUtils.isInnerCallInChain(expression)) {
        return true;
      }
      if (usesInject(psiClass)) {
        return true;
      }
      registerError(expression);
      return true;
    }

    private static boolean usesInject(PsiClass aClass) {
      for (PsiMethod method: aClass.getAllMethods()) {
        if (AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
          return true;
        }
      }
      for (PsiField field: aClass.getAllFields()) {
        if (AnnotationUtil.isAnnotated(field, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
          return true;
        }
      }
      return false;
    }
  }
}