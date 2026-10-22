// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.extensions.GuiceCallPattern;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

/**
 * Reports {@code requestStaticInjection()} calls for classes that have no static
 * {@code @Inject}-annotated fields or methods.
 */
public final class UnnecessaryStaticInjectionInspection extends BaseUastInspection {
  public UnnecessaryStaticInjectionInspection() {
    extendCall(
        GuiceCallPattern.named("requestStaticInjection").minArguments(1),
        UnnecessaryStaticInjectionInspection::checkRequestStaticInjection
    );
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("unnecessary.static.injection.problem.descriptor");
  }

  @Override
  public LocalQuickFix buildFix(PsiElement location, Object[] infos) {
    return new DeleteBindingFix(DeleteBindingFix.Mode.ARGUMENT, "remove.static.injection");
  }

  private static void checkRequestStaticInjection(@NotNull UCallExpression expression,
                                                  @NotNull BaseUastInspectionVisitor visitor) {
    for (UExpression arg : expression.getValueArguments()) {
      final PsiType classType = GuiceUtils.getBindingTypeFromExpression(arg);
      if (!(classType instanceof PsiClassType psiClassType)) {
        continue;
      }
      final PsiClass classToBindStatically = psiClassType.resolve();
      if (classToBindStatically == null) {
        continue;
      }
      if (!classHasStaticInjects(classToBindStatically)) {
        visitor.registerClassLiteralError(arg);
      }
    }
  }

  /**
   * Guice injects the static {@code @Inject} members of the class and of its superclasses
   * ({@code InjectionPoint.forStaticMethodsAndFields} walks the hierarchy).
   */
  private static boolean classHasStaticInjects(PsiClass aClass) {
    for (PsiMethod method : aClass.getAllMethods()) {
      if (method.hasModifierProperty(PsiModifier.STATIC) &&
          AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, 0)) {
        return true;
      }
    }
    for (PsiField field : aClass.getAllFields()) {
      if (field.hasModifierProperty(PsiModifier.STATIC) &&
          AnnotationUtil.isAnnotated(field, GuiceAnnotations.INJECTS, 0)) {
        return true;
      }
    }
    return false;
  }
}
