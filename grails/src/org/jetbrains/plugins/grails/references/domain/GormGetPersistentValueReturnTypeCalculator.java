// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.util.PairFunction;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

public class GormGetPersistentValueReturnTypeCalculator implements PairFunction<GrMethodCall, PsiMethod, PsiType> {
  @Override
  public PsiType fun(GrMethodCall methodCall, PsiMethod method) {
    GrExpression[] arguments = methodCall.getArgumentList().getExpressionArguments();
    if (arguments.length == 0) return null;
    
    if (!(arguments[0] instanceof GrLiteralImpl)) return null;
    
    Object value = ((GrLiteralImpl)arguments[0]).getValue();
    if (!(value instanceof String)) return null;

    PsiClass domainClass = ((GrLightMethodBuilder)method).getData();

    Pair<PsiType,PsiElement> pair = DomainDescriptor.getPersistentProperties(domainClass).get(value);
    if (pair == null) return null;
    
    return pair.first;
  }
}
