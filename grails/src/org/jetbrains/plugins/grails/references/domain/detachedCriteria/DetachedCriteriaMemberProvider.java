// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.detachedCriteria;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.domain.DomainDescriptor;
import org.jetbrains.plugins.grails.references.domain.DomainMembersProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class DetachedCriteriaMemberProvider extends NonCodeMembersContributor {
  @Override
  protected String getParentClassName() {
    return DetachedCriteriaUtil.DETACHED_CRITERIA_CLASS;
  }

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    String nameHint = ResolveUtil.getNameHint(processor);
    if (nameHint == null) return;

    if (!(place instanceof GrReferenceExpression refExp)) return;

    GrExpression qualifier = refExp.getQualifierExpression();
    if (qualifier == null) return;

    PsiClass domainClass = DetachedCriteriaUtil.getDomainClassByDetachedCriteriaExpression(qualifier.getType());
    if (domainClass == null) return;

    GrLightMethodBuilder finderMethod = DomainMembersProvider.parseFinderMethod(nameHint, DomainDescriptor.getDescriptor(domainClass));
    if (finderMethod != null) {
      processor.execute(finderMethod, state);
    }
  }

}
