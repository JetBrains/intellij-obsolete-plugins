// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

final class ConfigMemberContributor extends NonCodeMembersContributor {
  private static final String CLASS_SOURCE = "class BuildConfigMembers {" +
                                             "  public String getGrailsHome(){}" +
                                             "  public String getAppName() {}" +
                                             "  public String getAppVersion() {}" +
                                             "  public String getBasedir() {}" +
                                             "  public java.io.File getUserHome() {}" +
                                             "  public String getServletVersion() {}" +
                                             "}";

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!GrailsUtils.isConfigGroovyFile(place.getContainingFile().getOriginalFile())) return;
    if (!"Config".equals(TypesUtil.getQualifiedName(qualifierType))) return;

    DynamicMemberUtils.process(processor, false, place, CLASS_SOURCE);
  }
}
