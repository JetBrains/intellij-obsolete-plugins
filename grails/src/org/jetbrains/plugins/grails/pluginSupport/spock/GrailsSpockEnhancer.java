// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.spock;

import com.intellij.javaee.web.WebCommonClassNames;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.compiled.ClsFieldImpl;
import com.intellij.psi.impl.compiled.ClsMethodImpl;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.tests.GrailsTestUtils;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.typeEnhancers.GrReferenceTypeEnhancer;

import java.util.Map;

final class GrailsSpockEnhancer extends GrReferenceTypeEnhancer {
  private static Map<String, String> typeMap;

  public static Map<String, String> getTypeMap() {
    if (typeMap == null) {
      typeMap = GrailsUtils.createMap(
        "getForwardArgs", CommonClassNames.JAVA_UTIL_MAP,
        "getRedirectArgs", CommonClassNames.JAVA_UTIL_MAP,
        "getChainArgs", CommonClassNames.JAVA_UTIL_MAP,
        "webRequest", "org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest",
        "getMockRequest", WebCommonClassNames.JAVAX_HTTP_SERVLET_REQUEST,
        "getMockResponse", WebCommonClassNames.JAVAX_HTTP_SERVLET_RESPONSE,
        "getMockSession", WebCommonClassNames.JAVAX_HTTP_SESSION,
        "getRenderArgs", CommonClassNames.JAVA_UTIL_MAP,
        "getMockParams", "org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap",
        "getMockFlash", "org.codehaus.groovy.grails.web.servlet.FlashScope"
      );
    }
    return typeMap;
  }

  private static @Nullable PsiType getFromMap(String memberName, PsiElement context) {
    String type = getTypeMap().get(memberName);
    if (type == null) return null;

    return JavaPsiFacade.getElementFactory(context.getProject()).createTypeByFQClassName(type, context.getResolveScope());
  }

  @Override
  public PsiType getReferenceType(GrReferenceExpression ref, PsiElement resolved) {
    if (!(resolved instanceof ClsFieldImpl)) {
      if (!(resolved instanceof ClsMethodImpl) || ((ClsMethodImpl)resolved).getParameterList().getParametersCount() > 0) {
        return null;
      }
    }

    PsiClass containingClass = ((PsiMember)resolved).getContainingClass();
    if (containingClass == null) return null;

    String className = containingClass.getQualifiedName();
    String memberName = ((PsiNamedElement)resolved).getName();

    if ("grails.plugin.spock.ControllerSpec".equals(className)) {
      if ("getControllerClass".equals(memberName)) {
        return GrailsTestUtils.getTestedClassClass(ref);
      }

      if ("getController".equals(memberName)) {
        return GrailsTestUtils.getTestedClass(ref);
      }

      return getFromMap(memberName, ref);
    }

    if ("grails.plugin.spock.TagLibSpec".equals(className)) {
      if ("getTagLibClass".equals(memberName)) {
        return GrailsTestUtils.getTestedClassClass(ref);
      }

      if ("getTagLib".equals(memberName)) {
        return GrailsTestUtils.getTestedClass(ref);
      }

      return null;
    }

    if ("grails.plugin.spock.MvcSpec".equals(className)) {
      if ("classUnderTest".equals(memberName)) {
        return GrailsTestUtils.getTestedClassClass(ref);
      }

      if ("instanceUnderTest".equals(memberName)) {
        return GrailsTestUtils.getTestedClass(ref);
      }

      return getFromMap(memberName, ref);
    }

    if ("grails.plugin.spock.UnitSpec".equals(className)) {
      if ("errorsMap".equals(memberName)) {
        return JavaPsiFacade.getElementFactory(containingClass.getProject()).createTypeByFQClassName(CommonClassNames.JAVA_UTIL_MAP,
                                                                                                     ref.getResolveScope());
      }
    }

    return null;
  }

}
