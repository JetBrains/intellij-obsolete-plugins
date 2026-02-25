// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.javaee.web.WebCommonClassNames;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsStructure;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class HttpRequestMemberContributor extends NonCodeMembersContributor {
  @Override
  protected @Nullable String getParentClassName() {
    return WebCommonClassNames.JAVAX_HTTP_SERVLET_REQUEST;
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

    JavaPsiFacade facade = JavaPsiFacade.getInstance(project);

    PsiClassType httpRequestType = PsiTypesUtil.getClassType(aClass);

    PsiClass servletApi = facade.findClass("org.codehaus.groovy.grails.plugins.web.api.ServletRequestApi", resolveScope);
    if (servletApi != null) {
      if (!GrailsPsiUtil.enhance(processor, servletApi, httpRequestType)) return;
    }

    PsiClass mimeApi = facade.findClass("org.codehaus.groovy.grails.plugins.web.api.RequestMimeTypesApi", resolveScope);
    if (mimeApi != null) {
      if (!GrailsPsiUtil.enhance(processor, mimeApi, httpRequestType)) return;
    }

    String nameHint = ResolveUtil.getNameHint(processor);

    if (nameHint == null || "getXML".equals(nameHint)) { // injected in org.codehaus.groovy.grails.plugins.converters.ConvertersPluginSupport#enhanceRequest()
      PsiClass xmlConverter = facade.findClass("grails.converters.XML", resolveScope);
      if (xmlConverter != null) {
        PsiMethod parseMethod = createMethodByConverter(xmlConverter, "getXML");
        if (parseMethod != null) {
          if (!processor.execute(parseMethod, state)) return;
        }
      }
    }

    if (nameHint == null || "getJSON".equals(nameHint)) { // injected in org.codehaus.groovy.grails.plugins.converters.ConvertersPluginSupport#enhanceRequest()
      PsiClass jsonConverter = facade.findClass("grails.converters.JSON", resolveScope);
      if (jsonConverter != null) {
        PsiMethod parseMethod = createMethodByConverter(jsonConverter, "getJSON");
        if (parseMethod != null) {
          if (!processor.execute(parseMethod, state)) return;
        }
      }
    }
  }

  private static @Nullable PsiMethod createMethodByConverter(@NotNull PsiClass converterClass, @NotNull String methodName) {
    PsiMethod parseMethod = null;

    for (PsiMethod method : converterClass.findMethodsByName("parse", false)) {
      PsiParameter[] parameters = method.getParameterList().getParameters();
      if (parameters.length == 1 && parameters[0].getType().equalsToText(WebCommonClassNames.JAVAX_HTTP_SERVLET_REQUEST)) {
        parseMethod = method;
        break;
      }
    }

    if (parseMethod == null) return null;

    GrLightMethodBuilder builder = new GrLightMethodBuilder(converterClass.getManager(), methodName);
    builder.setNavigationElement(parseMethod);
    builder.setReturnType(parseMethod.getReturnType());
    builder.setContainingClass(converterClass);
    builder.addModifier(PsiModifier.PUBLIC);

    return builder;
  }
}
