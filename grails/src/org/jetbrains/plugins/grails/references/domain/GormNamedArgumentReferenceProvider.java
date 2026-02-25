// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.GrailsMethodNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

public class GormNamedArgumentReferenceProvider extends GrailsMethodNamedArgumentReferenceProvider.Contributor.Provider implements GrailsMethodNamedArgumentReferenceProvider.Contributor {
  @Override
  public void register(GrailsMethodNamedArgumentReferenceProvider registrar) {
    registrar.register("sort", this, new LightMethodCondition(DomainDescriptor.DOMAIN_DYNAMIC_METHOD), "list");
    registrar.register(0, this, new LightMethodCondition(DomainDescriptor.DOMAIN_DYNAMIC_METHOD), "isDirty");
  }

  @Override
  protected PsiReference[] createRef(@NotNull PsiElement element, @NotNull GroovyResolveResult resolveResult) {
    PsiClass domainClass = ((GrLightMethodBuilder)resolveResult.getElement()).getData();
    return new PsiReference[]{new GormPropertyReference(element, false, domainClass)};
  }

}
