// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.tests;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.compiled.ClsFieldImpl;
import com.intellij.psi.impl.compiled.ClsMethodImpl;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.typeEnhancers.GrReferenceTypeEnhancer;

public final class GrailsTestVariableEnhancer extends GrReferenceTypeEnhancer {
  @Override
  public PsiType getReferenceType(GrReferenceExpression ref, @Nullable PsiElement resolved) {
    if (!(resolved instanceof ClsFieldImpl)) {
      if (!(resolved instanceof ClsMethodImpl) || ((ClsMethodImpl)resolved).getParameterList().getParametersCount() > 0) {
        return null;
      }
    }

    PsiClass containingClass = ((PsiMember)resolved).getContainingClass();

    if (containingClass == null) return null;

    String className = containingClass.getQualifiedName();
    String memberName = ((PsiNamedElement)resolved).getName();

    if ("grails.test.ControllerUnitTestCase".equals(className)) {
      if ("controller".equals(memberName)) {
        return GrailsTestUtils.getTestedClass(ref);
      }
    }
    else if ("grails.test.TagLibUnitTestCase".equals(className)) {
      if ("tagLib".equals(memberName)) {
        return GrailsTestUtils.getTestedClass(ref);
      }
    }
    else if (!"grails.test.GrailsUnitTestCase".equals(className)) {
      return null;
    }

    // grails.test.GrailsUnitTestCase".equals(className) == true
    if ("getApplicationContext".equals(memberName)) {
      return JavaPsiFacade.getElementFactory(containingClass.getProject()).createTypeByFQClassName(
        "org.codehaus.groovy.grails.support.MockApplicationContext", ref.getResolveScope());
    }

    return null;
  }
}
