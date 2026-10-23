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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UClass;
import com.intellij.psi.PsiEnumConstant;
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
    return GuiceBundle.message("interception.annotation.without.runtime.retention.problem.descriptor", infos);
  }

  @Override
  public @NotNull AbstractUastNonRecursiveVisitor buildUastVisitor(@NotNull ProblemsHolder holder) {
    return new Visitor(this, holder);
  }

  private static class Visitor extends BaseUastInspectionVisitor {
    Visitor(@NotNull BaseUastInspection inspection, @NotNull ProblemsHolder holder) {
      super(inspection, holder);
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
      final UExpression argExpr = args.getFirst();
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
      if (!hasRuntimeRetention(operantAnnotation)) {
        registerClassLiteralError(argExpr, operantAnnotation.getName());
      }
      return true;
    }
  }

  /**
   * Tells if the annotation class keeps its instances at runtime. Reads {@code java.lang.annotation.Retention}
   * or {@code kotlin.annotation.Retention} and resolves the policy constant. Without a retention annotation a
   * Kotlin annotation class is RUNTIME and a Java one is CLASS.
   */
  static boolean hasRuntimeRetention(@NotNull PsiClass annotationClass) {
    UClass uClass = UastContextKt.toUElement(annotationClass, UClass.class);
    if (uClass != null) {
      for (UAnnotation annotation : uClass.getUAnnotations()) {
        if (RETENTION_ANNOTATIONS.contains(annotation.getQualifiedName())) {
          return isRuntimePolicy(annotation.findAttributeValue("value"));
        }
      }
      return "kotlin".equalsIgnoreCase(uClass.getLang().getID());
    }
    PsiAnnotation retention = AnnotationUtil.findAnnotation(annotationClass, RETENTION_ANNOTATIONS);
    return retention != null && retention.getText().contains("RUNTIME");
  }

  private static boolean isRuntimePolicy(@Nullable UExpression value) {
    if (value == null) return false;
    if (value instanceof UReferenceExpression reference && reference.resolve() instanceof PsiEnumConstant constant) {
      return "RUNTIME".equals(constant.getName());
    }
    return value.asSourceString().contains("RUNTIME");
  }
}