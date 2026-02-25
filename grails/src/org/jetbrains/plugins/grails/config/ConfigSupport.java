// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

public final class ConfigSupport extends GrailsAbstractConfigSupport {

  @Override
  public PropertiesProvider getProvider(@NotNull GroovyFile file) {
    if (GrailsUtils.isConfigGroovyFile(file)) {
      return this;
    }
    return null;
  }

  @Override
  public PropertiesProvider getConfigSlurperInfo(@NotNull PsiElement qualifierResolve) {
    if (!(qualifierResolve instanceof PsiMethod method)) return null;

    if (!"getConfig".equals(method.getName()) || method.getParameterList().getParametersCount() > 0) return null;

    PsiClass containingClass = method.getContainingClass();
    if (!InheritanceUtil.isInheritor(containingClass, "org.codehaus.groovy.grails.commons.GrailsApplication")
      && !InheritanceUtil.isInheritor(containingClass, "org.codehaus.groovy.grails.commons.ConfigurationHolder")) {
      return null;
    }

    return this;
  }

  @Override
  protected String @NotNull [] getFinalProperties() {
    return new String[]{
      "grails.config.locations",
      "grails.project.groupId",
      "grails.mime.file.extensions",
      "grails.mime.use.accept.header",
      "grails.mime.types",
      "grails.views.default.codec",
      "grails.views.gsp.encoding",
      "grails.converters.encoding",
      "grails.views.gsp.sitemesh.preprocess",
      "grails.scaffolding.templates.domainSuffix",
      "grails.json.legacy.builder",
      "grails.enable.native2ascii",
      "grails.logging.jul.usebridge",
      "grails.spring.bean.packages",
      "grails.exceptionresolver.params.exclude",
      "grails.exceptionresolver.logRequestParameters",
      "log4j",
      "log4j.main",
      "grails.serverURL",
      "grails.project.war.file",
      "grails.war.dependencies",
      "grails.war.java5.dependencies",
      "grails.war.copyToWebApp",
      "grails.war.resources"
    };
  }
}
