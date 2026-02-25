// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.seachable;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.domain.DomainDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.Map;

final class SearchableMemberMethodContributor extends ClosureMemberContributor {
  @Override
  protected void processMembers(@NotNull GrClosableBlock closure,
                                @NotNull PsiScopeProcessor processor,
                                @NotNull PsiElement place,
                                @NotNull ResolveState state) {
    if (!ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) return;

    PsiElement parent = closure.getParent();
    if (!(parent instanceof GrField field)) return;

    if (!GrailsSearchableUtil.isSearchableField(field)) return;

    if (!(place instanceof GrReferenceExpression refExpr)) return;

    if (refExpr.isQualified()) return;

    String nameHint = ResolveUtil.getNameHint(processor);

    if ("setExcept".equals(nameHint) || "setOnly".equals(nameHint)) {
      GrLightMethodBuilder builder = new GrLightMethodBuilder(field.getManager(), nameHint);
      builder.addParameter("propertiesList", CommonClassNames.JAVA_UTIL_LIST);
      processor.execute(builder, state);
      return;
    }

    PsiClass containingClass = field.getContainingClass();
    if (containingClass == null) {
      return;
    }
    Map<String, Pair<PsiType, PsiElement>> propertiesMap = DomainDescriptor.getDescriptor(containingClass).getPersistentProperties();

    if (nameHint == null) {
      for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : propertiesMap.entrySet()) {
        PsiMethod method = GrailsSearchableUtil.createMethod(entry.getKey(), entry.getValue().second, containingClass);
        if (!processor.execute(method, ResolveState.initial())) return;
      }

      if (!processor.execute(GrailsSearchableUtil.createAllMethod(field.getManager()), state)) return;
    }
    else {
      Pair<PsiType, PsiElement> pair = propertiesMap.get(nameHint);
      if (pair != null) {
        PsiMethod method = GrailsSearchableUtil.createMethod(nameHint, pair.second, containingClass);
        if (!processor.execute(method, state)) return;
      }
      else if ("all".equals(nameHint)) {
        if (!processor.execute(GrailsSearchableUtil.createAllMethod(field.getManager()), state)) return;
      }
    }
  }
}
