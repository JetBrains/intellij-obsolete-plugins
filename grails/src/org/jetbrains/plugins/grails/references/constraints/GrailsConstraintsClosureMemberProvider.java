// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.constraints;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMissingMethodContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class GrailsConstraintsClosureMemberProvider extends ClosureMissingMethodContributor {
  @Override
  public boolean processMembers(GrClosableBlock closure, PsiScopeProcessor processor, GrReferenceExpression refExpr, ResolveState state) {
    PsiElement eField = closure.getParent();
    if (!(eField instanceof GrField field)) return true;

    String fieldName = field.getName();

    if (!"constraints".equals(fieldName) || !field.hasModifierProperty(PsiModifier.STATIC)) return true;

    PsiClass aClass = field.getContainingClass();
    if (aClass == null || !GrailsUtils.isValidatedClass(aClass)) return true;

    String nameHint = ResolveUtil.getNameHint(processor);

    if (nameHint == null) {
      for (PsiField psiField : aClass.getAllFields()) {
        if (psiField.hasModifierProperty(PsiModifier.STATIC)) continue;

        PsiMethod method = GrailsConstraintsUtil.createMethod(psiField.getName(), psiField, psiField.getType(), aClass);
        if (!processor.execute(method, ResolveState.initial())) return false;
      }
    }
    else {
      PsiField psiField = aClass.findFieldByName(nameHint, true);
      if (psiField != null && !psiField.hasModifierProperty(PsiModifier.STATIC)) {
        PsiMethod method = GrailsConstraintsUtil.createMethod(nameHint, psiField, psiField.getType(), aClass);
        if (!processor.execute(method, ResolveState.initial())) return false;
      }
    }

    if (!GrailsConstraintsUtil.processImportFromMethod(processor, state, aClass, nameHint)) return false;

    return true;
  }
}
