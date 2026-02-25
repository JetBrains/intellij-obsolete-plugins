// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.constraints;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.typeEnhancers.AbstractClosureParameterEnhancer;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

public final class GrailsValidatorParamTypeEnhancer extends AbstractClosureParameterEnhancer {

  @Override
  protected PsiType getClosureParameterType(@NotNull GrFunctionalExpression expression, int index) {
    PsiElement eNamedArgument = expression.getParent();
    if (!(eNamedArgument instanceof GrNamedArgument) || !"validator".equals(((GrNamedArgument)eNamedArgument).getLabelName())) return null;

    PsiElement constraintMethodCall = PsiUtil.getCallByNamedParameter((GrNamedArgument)eNamedArgument);
    if (!(constraintMethodCall instanceof GrMethodCall)) return null;

    PsiMethod constraintMethod = ((GrMethodCall)constraintMethodCall).resolveMethod();
    if (!GrailsConstraintsUtil.isConstraintsMethod(constraintMethod)) return null;

    return switch (index) {
      case 0 -> GrailsConstraintsUtil.getValidatedValueType(constraintMethod);
      case 1 -> {
        PsiClass validatedClass = GrailsConstraintsUtil.getValidatedClass(constraintMethod);
        if (validatedClass == null) yield null;
        yield PsiTypesUtil.getClassType(validatedClass);
      }
      case 2 -> TypesUtil.createTypeByFQClassName("org.springframework.validation.Errors", constraintMethodCall);
      default -> null;
    };
  }
}
