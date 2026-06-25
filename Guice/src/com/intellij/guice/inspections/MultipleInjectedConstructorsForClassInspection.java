// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

/**
 * Reports classes that have more than one constructor annotated with {@code @Inject}.
 * Guice requires at most one {@code @Inject} constructor per class.
 *
 * <p>Example:
 * <pre>
 * // Flagged: two @Inject constructors
 * class Foo {
 *     {@literal @}Inject Foo(Bar bar) {}
 *     {@literal @}Inject Foo(Bar bar, Baz baz) {}
 * }
 *
 * // OK: single @Inject constructor
 * class Foo {
 *     {@literal @}Inject Foo(Bar bar) {}
 *     Foo(Bar bar, Baz baz) {}  // no @Inject
 * }
 * </pre>
 */
public final class MultipleInjectedConstructorsForClassInspection extends BaseUastInspection {
  public MultipleInjectedConstructorsForClassInspection() {
    super(UMethod.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("multiple.injected.constructors.for.class.problem.descriptor");
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
    public boolean visitMethod(@NotNull UMethod uMethod) {
      final PsiMethod method = uMethod.getJavaPsi();
      if (!method.isConstructor()) {
        return true;
      }
      final PsiClass containingClass = method.getContainingClass();
      if (containingClass == null) {
        return true;
      }
      final PsiMethod[] constructors = containingClass.getConstructors();
      if (constructors.length <= 1) {
        return true;
      }
      if (!AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
        return true;
      }

      int annotatedConstructorCount = 0;
      for (PsiMethod constructor : constructors) {
        if (AnnotationUtil.isAnnotated(constructor, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
          annotatedConstructorCount++;
        }
      }
      if (annotatedConstructorCount > 1) {
        registerMethodError(uMethod);
      }
      return true;
    }
  }
}