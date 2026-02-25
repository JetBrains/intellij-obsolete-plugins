// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

public abstract class MemberProvider {

  public void processMembers(PsiScopeProcessor processor, PsiClass psiClass, PsiElement place) {
    if (place instanceof GrReferenceExpression) {
      processMembers(processor, psiClass, (GrReferenceExpression)place);
    }
  }

  public void processMembers(PsiScopeProcessor processor, PsiClass psiClass, GrReferenceExpression ref) {

  }

}
