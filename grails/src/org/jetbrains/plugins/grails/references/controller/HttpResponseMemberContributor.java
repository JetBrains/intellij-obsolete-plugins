// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.javaee.web.WebCommonClassNames;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsStructure;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;

final class HttpResponseMemberContributor extends NonCodeMembersContributor {
  @Override
  protected @Nullable String getParentClassName() {
    return WebCommonClassNames.JAVAX_HTTP_SERVLET_RESPONSE;
  }

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (aClass == null) return;

    GrailsStructure grailsStructure = GrailsStructure.getInstance(place);
    if (grailsStructure == null) return;

    Project project = aClass.getProject();

    GlobalSearchScope resolveScope = place.getResolveScope();

    PsiClassType httpResponseType = PsiTypesUtil.getClassType(aClass);

    PsiClass responceMimeApi = JavaPsiFacade.getInstance(project).findClass("org.codehaus.groovy.grails.plugins.web.api.ResponseMimeTypesApi", resolveScope);
    if (responceMimeApi != null) {
      if (!GrailsPsiUtil.enhance(processor, responceMimeApi, httpResponseType)) return;
    }
  }
}
