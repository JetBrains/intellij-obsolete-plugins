// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.detachedCriteria;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.source.PsiImmediateClassType;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.domain.DomainDescriptor;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

import java.util.Map;

public final class DetachedCriteriaUtil {

  public static final String DETACHED_CRITERIA_CLASS = "grails.gorm.DetachedCriteria";

  private DetachedCriteriaUtil() {
  }

  public static boolean isDomainDetachedCriteriaMethod(@NotNull PsiMethod method) {
    if (GrLightMethodBuilder.checkKind(method, DomainDescriptor.DOMAIN_DYNAMIC_METHOD)) {
      String methodName = method.getName();
      return "where".equals(methodName) || "findAll".equals(methodName) || "find".equals(methodName);
    }

    return false;
  }

  public static @Nullable PsiClass getDomainFromSubstitutor(@NotNull GroovyResolveResult resolveResult) {
    PsiSubstitutor substitutor = resolveResult.getSubstitutor();
    Map<PsiTypeParameter,PsiType> substitutionMap = substitutor.getSubstitutionMap();
    if (substitutionMap.size() != 1) return null;

    PsiClass res = PsiTypesUtil.getPsiClass(substitutionMap.values().iterator().next());
    return GrailsArtifact.DOMAIN.isInstance(res) ? res : null;
  }

  public static @Nullable PsiClass getDomainClassByDetachedCriteriaExpression(@Nullable PsiType type) {
    if (!(type instanceof PsiImmediateClassType)) return null;

    PsiClass detachedCriteriaClass = PsiTypesUtil.getPsiClass(type);
    if (detachedCriteriaClass == null || !DETACHED_CRITERIA_CLASS.equals(detachedCriteriaClass.getQualifiedName())) {
      return null;
    }

    PsiType[] parameters = ((PsiImmediateClassType)type).getParameters();
    if (parameters.length != 1) return null;

    PsiClass domainClass = PsiTypesUtil.getPsiClass(parameters[0]);
    if (!GrailsArtifact.DOMAIN.isInstance(domainClass)) return null;

    return domainClass;
  }
}
