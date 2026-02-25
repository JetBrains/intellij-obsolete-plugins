// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.criteria;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.GrailsMethodNamedArgumentReferenceProvider;
import org.jetbrains.plugins.grails.references.domain.GormPropertyReference;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

public class CriteriaPropertyReferenceProvider extends GrailsMethodNamedArgumentReferenceProvider.Contributor.Provider implements GrailsMethodNamedArgumentReferenceProvider.Contributor {

  @Override
  public void register(GrailsMethodNamedArgumentReferenceProvider registrar) {
    Condition<PsiMethod> condition = Conditions.or(
      new ClassSourceCondition(CriteriaBuilderImplicitMemberContributor.CLASS_SOURCE),
      new ClassNameCondition(CriteriaBuilderUtil.CRITERIA_BUILDER_CLASS)
    );

    registrar.register(0, this, condition,
                       // #CHECK# grails.orm.HibernateCriteriaBuilder
                       "property", "distinct", "avg", "calculatePropertyName", "count", "countDistinct", "groupProperty", "max", "min",
                       "sum", "gt", "ge", "lt", "le", "eq", "like", "rlike", "ilike", "in", "inList", "order", "sizeEq", "sizeGt", "sizeGe",
                       "sizeLe", "sizeLt", "sizeNe", "ne", "notEqual", "between", "fetchMode",

                           // From HibernateCriteriaBuilder.invokeMethod(...)
                       "isNull", "isNotNull", "isEmpty", "isNotEmpty");

    String[] twoArgumentMethods = {"eqProperty", "neProperty", "gtProperty", "geProperty", "ltProperty", "leProperty"};
    registrar.register(0, this, condition, twoArgumentMethods);
    registrar.register(1, this, condition, twoArgumentMethods);
  }

  @Override
  public PsiReference[] createRef(@NotNull PsiElement element,
                                  @NotNull GrMethodCall methodCall,
                                  int argumentIndex,
                                  @NotNull GroovyResolveResult resolveResult) {
    PsiClass domainClass = CriteriaBuilderUtil.findDomainClassByMethodCall(methodCall, false);
    if (domainClass == null) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{new GormPropertyReference(element, false, domainClass)};
  }

}
