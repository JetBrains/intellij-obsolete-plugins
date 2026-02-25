// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.constraints;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

final class GrailsValidationMethodsContributor extends NonCodeMembersContributor {
  // #CHECK# See WebMetaUtils.enhanceCommandObject()
  private static final String CLASS_SOURCES = "class CommandObjectClass {" +
                                              " private void setErrors(org.springframework.validation.Errors errors) {}" +
                                              " private org.springframework.validation.Errors getErrors() {}" +
                                              " private boolean hasErrors() {}" +
                                              " private boolean validate() {}" +
                                              "}";

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) return;

    if (aClass == null || !GrailsUtils.isValidatedClass(aClass)) return;

    String nameHint = ResolveUtil.getNameHint(processor);

    for (PsiMethod method : DynamicMemberUtils.getMembers(aClass.getProject(), CLASS_SOURCES).getDynamicMethods(nameHint)) {
      if (!processor.execute(method, state)) return;
    }
  }
}
