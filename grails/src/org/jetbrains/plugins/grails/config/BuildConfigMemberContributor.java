// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

final class BuildConfigMemberContributor extends NonCodeMembersContributor {
  // #CHECK# see BuildSettings.createConfigSlurper()
  private static final String CLASS_SOURCE = "class BuildConfigMembers {" +
                                             "  public String getBasedir() {}" +
                                             "  public java.io.File getBaseFile() {}" +
                                             "  public String getBaseName() {}" +
                                             "  public String getGrailsHome(){}" +
                                             "  public String getGrailsVersion() {}" +
                                             "  public java.io.File getUserHome() {}" +
                                             "  public grails.util.BuildSettings getGrailsSettings() {}" +
                                             "  public String getAppName() {}" +
                                             "  public String getAppVersion() {}" +
                                             "}";

  @Override
  protected String getParentClassName() {
    return "BuildConfig";
  }

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!GrailsUtils.isBuildConfigFile(place.getContainingFile())) return;

    DynamicMemberUtils.process(processor, false, place, CLASS_SOURCE);
  }

}
