// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references;

import com.intellij.openapi.util.Trinity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.pluginSupport.shiro.GrailsShiroAccessControlMethodProvider;
import org.jetbrains.plugins.grails.references.domain.GormConstraintMethodProvider;
import org.jetbrains.plugins.grails.references.domain.GormMappingMethodProvider;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.grails.references.filter.FilterClosureMemberProvider;
import org.jetbrains.plugins.grails.references.jobs.JobClosureMethodProvider;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMissingMethodContributor;

import java.util.HashMap;
import java.util.Map;

public final class GrailsClosureMemberContributor extends ClosureMissingMethodContributor {
  private static final Map<String, Trinity<GrailsArtifact, Boolean, ? extends MethodProvider>> map = new HashMap<>();
  static {
    map.put("triggers", Trinity.create(GrailsArtifact.JOB, true, new JobClosureMethodProvider()));
    map.put("filters", Trinity.create(GrailsArtifact.FILTER, false, new FilterClosureMemberProvider()));

    map.put("constraints", Trinity.create(GrailsArtifact.DOMAIN, true, new GormConstraintMethodProvider()));
    map.put("mapping", Trinity.create(GrailsArtifact.DOMAIN, true, new GormMappingMethodProvider()));
    map.put("accessControl", Trinity.create(GrailsArtifact.CONTROLLER, true, new GrailsShiroAccessControlMethodProvider()));
  }

  @Override
  public boolean processMembers(GrClosableBlock closure, PsiScopeProcessor processor, GrReferenceExpression refExpr, ResolveState state) {
    PsiElement eField = closure.getParent();
    if (!(eField instanceof GrField field)) return true;

    String fieldName = field.getName();

    Trinity<GrailsArtifact, Boolean, ? extends MethodProvider> trinity = map.get(fieldName);
    if (trinity == null) return true;

    PsiClass aClass = field.getContainingClass();
    if (!trinity.first.isInstance(aClass) && !(trinity.first == GrailsArtifact.DOMAIN && GormUtils.isStandaloneGormBean(aClass))) {
      return true;
    }

    if (trinity.second && !field.hasModifierProperty(PsiModifier.STATIC)) return true;

    if (!trinity.third.processMembers(closure, aClass, processor, refExpr, state)) return false;

    return true;
  }

  public interface MethodProvider {
    boolean processMembers(@NotNull GrClosableBlock closure, PsiClass artifactClass, PsiScopeProcessor processor, GrReferenceExpression refExpr, ResolveState state);
  }
}
