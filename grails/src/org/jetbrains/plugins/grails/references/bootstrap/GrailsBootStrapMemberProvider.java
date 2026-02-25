// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.bootstrap;

import com.intellij.psi.PsiClass;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.plugins.grails.references.MemberProvider;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

public class GrailsBootStrapMemberProvider extends MemberProvider {

  @Override
  public void processMembers(PsiScopeProcessor processor, PsiClass psiClass, GrReferenceExpression ref) {
    GrField initField = PsiTreeUtil.getParentOfType(ref, GrField.class);

    if (initField == null || initField.getContainingClass() != psiClass) return;

    GrailsUtils.processEnvironmentDefinition(processor, ref);
  }
}
