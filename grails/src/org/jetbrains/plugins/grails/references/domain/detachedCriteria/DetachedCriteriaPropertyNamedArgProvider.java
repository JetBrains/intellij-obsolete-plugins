// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.detachedCriteria;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.domain.GormPersistentPropertiesNamedArgProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

public class DetachedCriteriaPropertyNamedArgProvider extends GormPersistentPropertiesNamedArgProvider {

  @Override
  protected PsiClass getDomainClass(@NotNull GrMethodCall call, PsiMethod resolve, GroovyResolveResult resolveResult) {
    if (GrLightMethodBuilder.checkKind(resolve, DetachedCriteriaClosureMemberProvider.MARKER)) {
      return ((GrLightMethodBuilder)resolve).getData();
    }

    return DetachedCriteriaUtil.getDomainFromSubstitutor(resolveResult);
  }
}
