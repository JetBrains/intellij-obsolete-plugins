// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.AnnotationUtils;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

/**
 * Reports redundant {@code .toProvider()} bindings where the provider class is the same as
 * the one already declared via {@code @ProvidedBy} on the bound type.
 *
 * <p>Example:
 * <pre>
 * // Flagged: @ProvidedBy already specifies FooProvider
 * {@literal @}ProvidedBy(FooProvider.class)
 * class Foo {}
 * bind(Foo.class).toProvider(FooProvider.class);  // redundant
 *
 * // OK: different provider
 * bind(Foo.class).toProvider(SpecialFooProvider.class);
 * </pre>
 */
public final class RedundantToProviderBindingInspection extends BaseUastInspection {
  public RedundantToProviderBindingInspection() {
    super(UCallExpression.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("redundant.to.provider.binding.problem.descriptor");
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
      if (!"toProvider".equals(expression.getMethodName())) {
        return true;
      }
      PsiClass referentClass = GuiceUtils.resolveClassArgument(expression);
      if (referentClass == null) {
        return true;
      }
      final PsiClass boundClass = GuiceUtils.findImplementedClassForBinding(expression);
      if (boundClass == null) {
        return true;
      }
      if (!AnnotationUtil.isAnnotated(boundClass, GuiceAnnotations.PROVIDED_BY, CHECK_HIERARCHY)) {
        return true;
      }
      final PsiAnnotation providedByAnnotation = boundClass.getModifierList().findAnnotation(GuiceAnnotations.PROVIDED_BY);
      if (providedByAnnotation != null) {
        final PsiElement defaultValue = AnnotationUtils.findDefaultValue(providedByAnnotation);
        if (defaultValue == null) {
          return true;
        }
        if (!(defaultValue instanceof PsiClassObjectAccessExpression)) {
          return true;
        }
        final PsiTypeElement providedByClassElement = ((PsiClassObjectAccessExpression)defaultValue).getOperand();
        final PsiType providedByClassType = providedByClassElement.getType();
        if (!(providedByClassType instanceof PsiClassType)) {
          return true;
        }
        final PsiClass providedByClass = ((PsiClassType)providedByClassType).resolve();
        if (referentClass.equals(providedByClass)) {
          registerError(expression);
        }
      }
      return true;
    }
  }
}