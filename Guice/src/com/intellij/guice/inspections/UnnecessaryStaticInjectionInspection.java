// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClassLiteralExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

import java.util.List;

/**
 * Reports {@code requestStaticInjection()} calls for classes that have no static
 * {@code @Inject}-annotated fields or methods. Without any static injection points,
 * the call is unnecessary and can be removed.
 *
 * <p>Example:
 * <pre>
 * // Flagged: Foo has no static @Inject members
 * requestStaticInjection(Foo.class);
 *
 * // OK: Foo has a static @Inject field
 * class Foo {
 *     {@literal @}Inject static Logger logger;
 * }
 * requestStaticInjection(Foo.class);
 * </pre>
 */
public final class UnnecessaryStaticInjectionInspection extends BaseUastInspection {
  public UnnecessaryStaticInjectionInspection() {
    super(UCallExpression.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("unnecessary.static.injection.problem.descriptor");
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
      if (!"requestStaticInjection".equals(methodName)) {
        return true;
      }
      final List<UExpression> args = expression.getValueArguments();
      for (UExpression arg : args) {
        if (!(arg instanceof UClassLiteralExpression classLiteral)) {
          continue;
        }
        final PsiType classType = classLiteral.getType();
        if (!(classType instanceof PsiClassType)) {
          continue;
        }
        final PsiClass classToBindStatically = ((PsiClassType)classType).resolve();
        if (classToBindStatically == null) {
          continue;
        }
        if (!classHasStaticInjects(classToBindStatically)) {
          registerError(arg);
        }
      }
      return true;
    }

    private static boolean classHasStaticInjects(PsiClass aClass) {
      final PsiMethod[] methods = aClass.getMethods();
      for (PsiMethod method : methods) {
        if (method.hasModifierProperty(PsiModifier.STATIC) &&
            AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
          return true;
        }
      }
      final PsiField[] fields = aClass.getFields();
      for (PsiField field : fields) {
        if (field.hasModifierProperty(PsiModifier.STATIC) &&
            AnnotationUtil.isAnnotated(field, GuiceAnnotations.INJECTS, AnnotationUtil.CHECK_HIERARCHY)) {
          return true;
        }
      }
      return false;
    }
  }
}