// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.spring;

import com.intellij.spring.references.SpringBeanNamesReferenceProvider;
import org.jetbrains.plugins.grails.references.GrailsMethodNamedArgumentReferenceProvider;

public class GrailsSpringMethodReferenceProvider implements GrailsMethodNamedArgumentReferenceProvider.Contributor {
  @Override
  public void register(GrailsMethodNamedArgumentReferenceProvider registrar) {
    ProviderAdapter refProfider = new ProviderAdapter(new SpringBeanNamesReferenceProvider());

    registrar.register(0, refProfider, new LightMethodCondition(GrailsResourcesGroovyMemberContributor.REF_METHOD_KIND), "ref");
    registrar.register(0, refProfider, new ClassNameCondition(GrailsResourcesGroovyMemberContributor.BEAN_BUILDER), "getBeanDefinition");

    ClassNameCondition runtimeSpringCfg = new ClassNameCondition("org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration");
    registrar.register(0, refProfider, runtimeSpringCfg, "containsBean");
    registrar.register(0, refProfider, runtimeSpringCfg, "getBeanConfig");
    registrar.register(1, refProfider, runtimeSpringCfg, "addAlias");
    registrar.register(0, refProfider, runtimeSpringCfg, "getBeanDefinition");


  }
}
