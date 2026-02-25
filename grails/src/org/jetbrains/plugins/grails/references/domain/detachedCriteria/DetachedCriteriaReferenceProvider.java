// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.detachedCriteria;

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.GrailsMethodNamedArgumentReferenceProvider;
import org.jetbrains.plugins.grails.references.domain.GormPropertyReference;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

public class DetachedCriteriaReferenceProvider extends GrailsMethodNamedArgumentReferenceProvider.Contributor.Provider implements GrailsMethodNamedArgumentReferenceProvider.Contributor {
  @Override
  public void register(GrailsMethodNamedArgumentReferenceProvider registrar) {
    Condition<PsiMethod> condition = new ClassNameCondition(DetachedCriteriaUtil.DETACHED_CRITERIA_CLASS);

    registrar.register(0, this, condition, "in", "order", "inList", "sizeEq", "sizeGt", "sizeGe", "sizeLe", "sizeLt", "sizeNe",
                       "isEmpty", "isNotEmpty", "isNull", "isNotNull", "eq", "idEq", "ne", "between", "gte", "ge", "gt", "lte", "le", "lt",
                       "like", "ilike", "rlike", "eqAll", "gtAll", "ltAll", "geAll", "leAll", "eqAll", "gtAll", "ltAll", "geAll", "leAll",
                       "sort", "property");

    registrar.register(-1, this, condition, "eqProperty", "neProperty", "gtProperty", "geProperty", "ltProperty", "leProperty");

    registrar.register(0,
                       this,
                       new LightMethodCondition(DetachedCriteriaClosureMemberProvider.PROJECTION_METHOD_MARKER),
                       "avg", "max", "min", "sum", "property", "distinct", "countDistinct");

    registrar.register("sort", this, condition, "list", "get", "count");
  }

  @Override
  protected PsiReference[] createRef(@NotNull PsiElement element, @NotNull GroovyResolveResult resolveResult) {
    PsiElement method = resolveResult.getElement();

    PsiClass domainClass;

    if (GrLightMethodBuilder.checkKind(method,
                                       DetachedCriteriaClosureMemberProvider.MARKER,
                                       DetachedCriteriaClosureMemberProvider.PROJECTION_METHOD_MARKER)) {
      domainClass = ((GrLightMethodBuilder)method).getData();
    }
    else {
      domainClass = DetachedCriteriaUtil.getDomainFromSubstitutor(resolveResult);
      if (domainClass == null) return PsiReference.EMPTY_ARRAY;
    }

    return new PsiReference[]{new GormPropertyReference(element, false, domainClass)};
  }

}
