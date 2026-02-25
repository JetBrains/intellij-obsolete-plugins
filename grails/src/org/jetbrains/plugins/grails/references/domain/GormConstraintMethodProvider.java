// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.GrailsClosureMemberContributor;
import org.jetbrains.plugins.grails.references.constraints.GrailsConstraintsUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.Map;

public final class GormConstraintMethodProvider implements GrailsClosureMemberContributor.MethodProvider {
  @Override
  public boolean processMembers(@NotNull GrClosableBlock closure,
                                PsiClass artifactClass,
                                PsiScopeProcessor processor,
                                GrReferenceExpression refExpr,
                                ResolveState state) {
    String nameHint = ResolveUtil.getNameHint(processor);

    Map<String, Pair<PsiType, PsiElement>> propertiesMap = DomainDescriptor.getDescriptor(artifactClass).getPropertiesWithTransients();

    if (nameHint == null) {
      for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : propertiesMap.entrySet()) {
        PsiMethod method = GrailsConstraintsUtil.createMethod(entry.getKey(), entry.getValue().second, entry.getValue().first, artifactClass);
        if (!processor.execute(method, ResolveState.initial())) return false;
      }
    }
    else {
      Pair<PsiType, PsiElement> pair = propertiesMap.get(nameHint);
      if (pair != null) {
        PsiMethod method = GrailsConstraintsUtil.createMethod(nameHint, pair.second, pair.first, artifactClass);
        if (!processor.execute(method, ResolveState.initial())) return false;
      }
    }

    if (!GrailsConstraintsUtil.processImportFromMethod(processor, state, artifactClass, nameHint)) return false;

    return true;
  }
}
