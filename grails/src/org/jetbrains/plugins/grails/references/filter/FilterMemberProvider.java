// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.filter;

import com.intellij.psi.PsiClass;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.plugins.grails.references.MemberProvider;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

public class FilterMemberProvider extends MemberProvider {

  private static final String CLASS_SOURCE = "class FilterElements {" +
                                             GrailsUtils.COMMON_WEB_PROPERTIES +
                                             " private final org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext applicationContext;" +
                                             " private void redirect(Map params){def z = params.uri + params.url + params.controller + params.action + params.id + params.fragment + params.params}" +
                                             " private void render(Closure cl){}" +
                                             " private void render(Map params, Closure cl = null){def z = params.text + params.builder " +
                                             "+ params.view + params.template + params.var + params.bean + params.model + params.collection " +
                                             "+ params.contentType + params.encoding + params.converter + params.plugin + params.status + params.contextPath}" +
                                             " private void render(String text){}" +
                                             " private void render(org.codehaus.groovy.grails.web.converters.Converter converter){}" +
                                             "}";

  @Override
  public void processMembers(PsiScopeProcessor processor, PsiClass psiClass, GrReferenceExpression ref) {
    DynamicMemberUtils.process(processor, psiClass, ref, CLASS_SOURCE);
  }
}
