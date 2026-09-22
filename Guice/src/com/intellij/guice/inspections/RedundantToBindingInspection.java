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
 * Reports redundant {@code .to()} bindings where the target class is the same as the bound
 * class (a self-binding), or where the target matches the class already specified by an
 * {@code @ImplementedBy} annotation on the bound type.
 *
 * <p>Example:
 * <pre>
 * // Flagged: self-binding is redundant
 * bind(Foo.class).to(Foo.class);
 *
 * // Flagged: @ImplementedBy already points to FooImpl
 * {@literal @}ImplementedBy(FooImpl.class)
 * interface Foo {}
 * bind(Foo.class).to(FooImpl.class);  // redundant
 *
 * // OK: binding to a different implementation
 * bind(Foo.class).to(SpecialFoo.class);
 * </pre>
 */
public final class RedundantToBindingInspection extends BaseUastInspection {
  public RedundantToBindingInspection() {
    super(UCallExpression.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("redundant.to.binding.problem.descriptor");
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
      if (!"to".equals(expression.getMethodName())) {
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
      if (GuiceUtils.findCallInChain(expression, "annotatedWith") != null) {
        return true;
      }
      if (AnnotationUtil.isAnnotated(boundClass, GuiceAnnotations.PROVIDED_BY, CHECK_HIERARCHY)) {
        return true;
      }
      if (AnnotationUtil.isAnnotated(boundClass, GuiceAnnotations.IMPLEMENTED_BY, CHECK_HIERARCHY)) {
        PsiModifierList modifierList = boundClass.getModifierList();
        if (modifierList == null) return true;
        final PsiAnnotation implementedByAnnotation = modifierList.findAnnotation(GuiceAnnotations.IMPLEMENTED_BY);
        if (implementedByAnnotation == null) return true;
        final PsiElement defaultValue = AnnotationUtils.findDefaultValue(implementedByAnnotation);
        if (defaultValue == null) {
          return true;
        }
        if (!(defaultValue instanceof PsiClassObjectAccessExpression)) {
          return true;
        }
        final PsiTypeElement implementByClass = ((PsiClassObjectAccessExpression)defaultValue).getOperand();
        final PsiType implmenetedByClass = implementByClass.getType();
        if (!(implmenetedByClass instanceof PsiClassType)) {
          return true;
        }
        final PsiClass implementedByClass = ((PsiClassType)implmenetedByClass).resolve();
        if (referentClass.equals(implementedByClass)) {
          registerError(expression);
        }
      }
      else {
        if (boundClass.equals(referentClass)) {
          registerError(expression);
        }
      }
      return true;
    }
  }
}