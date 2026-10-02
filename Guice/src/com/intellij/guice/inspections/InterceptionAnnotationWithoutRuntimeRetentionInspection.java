// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

import java.util.List;
import java.util.Set;

/**
 * Reports annotation classes passed to {@code Matchers.annotatedWith()} that do not have
 * {@code @Retention(RetentionPolicy.RUNTIME)}. Guice method interception matches annotations
 * at runtime via reflection, so non-runtime-retained annotations will never match.
 *
 * <p>Example:
 * <pre>
 * // Flagged: MyAnnotation lacks @Retention(RUNTIME)
 * bindInterceptor(
 *     Matchers.any(),
 *     Matchers.annotatedWith(MyAnnotation.class),
 *     new MyInterceptor()
 * );
 *
 * // OK: annotation has RUNTIME retention
 * {@literal @}Retention(RetentionPolicy.RUNTIME)
 * {@literal @}interface MyAnnotation {}
 * </pre>
 */
public final class InterceptionAnnotationWithoutRuntimeRetentionInspection extends BaseUastInspection {
  private static final Set<String> RETENTION_ANNOTATIONS =
      Set.of("java.lang.annotation.Retention", "kotlin.annotation.Retention");

  public InterceptionAnnotationWithoutRuntimeRetentionInspection() {
    super(UCallExpression.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("interception.annotation.without.runtime.retention.problem.descriptor");
  }

  @Override
  public @NotNull AbstractUastNonRecursiveVisitor buildUastVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new Visitor(this, holder, isOnTheFly);
  }

  private static class Visitor extends BaseUastInspectionVisitor {
    Visitor(@NotNull BaseUastInspection inspection, @NotNull ProblemsHolder holder, boolean onTheFly) {
      super(inspection, holder, onTheFly);
    }

    @Override
    public boolean visitCallExpression(@NotNull UCallExpression expression) {
      final String name = expression.getMethodName();
      if (!"annotatedWith".equals(name)) {
        return true;
      }
      final List<UExpression> args = expression.getValueArguments();
      if (args.size() != 1) {
        return true;
      }
      final UExpression argExpr = args.get(0);
      final PsiType annotationType = GuiceUtils.getBindingTypeFromExpression(argExpr);
      if (!(annotationType instanceof PsiClassType annotationClassType)) {
        return true;
      }
      final PsiMethod method = expression.resolve();
      if (method == null) {
        return true;
      }
      final PsiClass containingClass = method.getContainingClass();
      if (containingClass == null || !"com.google.inject.matcher.Matchers".equals(containingClass.getQualifiedName())) {
        return true;
      }
      final PsiClass operantAnnotation = annotationClassType.resolve();
      if (operantAnnotation == null || !operantAnnotation.isAnnotationType()) {
        return true;
      }
      final PsiAnnotation retentionAnnotation =
          AnnotationUtil.findAnnotation(operantAnnotation, RETENTION_ANNOTATIONS);
      if (retentionAnnotation == null || !retentionAnnotation.getText().contains("RUNTIME")) {
        registerError(argExpr);
      }
      return true;
    }
  }
}