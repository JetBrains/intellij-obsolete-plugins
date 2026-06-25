// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

/**
 * Reports {@code .to()} bindings that target a class Guice cannot instantiate
 * (e.g., an abstract class or interface with no {@code @Inject} constructor),
 * unless a {@code @Provides} method supplies it.
 *
 * <p>Example:
 * <pre>
 * // Flagged: AbstractService cannot be instantiated
 * bind(Service.class).to(AbstractService.class);
 *
 * // OK: ConcreteService has an @Inject constructor
 * bind(Service.class).to(ConcreteService.class);
 * </pre>
 */
public final class UninstantiableBindingInspection extends BaseUastInspection {
  public UninstantiableBindingInspection() {
    super(UCallExpression.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("uninstantiable.binding.problem.descriptor");
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
      if (!"to".equals(expression.getMethodName())) {
        return true;
      }
      PsiClass referentClass = GuiceUtils.resolveClassArgument(expression);
      if (referentClass == null) {
        return true;
      }
      if (GuiceUtils.isInstantiable(referentClass)) {
        return true;
      }
      final PsiMethod method = expression.resolve();
      if (method == null) {
        return true;
      }
      final PsiClass containingClass = method.getContainingClass();
      if (containingClass == null || !"com.google.inject.binder.LinkedBindingBuilder".equals(containingClass.getQualifiedName())) {
        return true;
      }
      UClass moduleUClass = UastUtils.getParentOfType(expression, UClass.class);
      PsiClass moduleClass = moduleUClass != null ? moduleUClass.getJavaPsi() : null;
      if (moduleClass != null && GuiceUtils.provides(moduleClass, referentClass)) {
        return true;
      }
      registerError(expression);
      return true;
    }
  }
}