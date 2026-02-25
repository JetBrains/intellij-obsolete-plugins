// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.filter;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

public final class GrailsFilterUtil {

  private GrailsFilterUtil() {
  }

  public static boolean isFilterDefinitionMethod(PsiElement methodCall) {
    if (!(methodCall instanceof GrMethodCall)) return false;

    PsiClass aClass = PsiTreeUtil.getParentOfType(methodCall, PsiClass.class);
    if (!GrailsArtifact.FILTER.isInstance(aClass)) return false;

    PsiMethod method = ((GrMethodCall)methodCall).resolveMethod();

    return FilterClosureMemberProvider.isFilterDefinitionMethod(method);
  }

  //public static boolean isFilterClosure(GrClosableBlock closure) {
  //  PsiElement eField = closure.getParent();
  //
  //  if (!(eField instanceof GrField)) return false;
  //  GrField field = (GrField)eField;
  //
  //  if (!"filters".equals(field.getName()) || field.getDeclaredType() != null) return false;
  //
  //  PsiClass aClass = field.getContainingClass();
  //
  //  return GrailsArtifact.FILTER.isInstance(aClass);
  //}
  //
}
