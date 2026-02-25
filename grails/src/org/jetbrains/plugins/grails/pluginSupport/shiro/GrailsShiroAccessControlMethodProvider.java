// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.shiro;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.GrailsClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

public class GrailsShiroAccessControlMethodProvider implements GrailsClosureMemberContributor.MethodProvider {

  @Override
  public boolean processMembers(@NotNull GrClosableBlock closure,
                                PsiClass artifactClass,
                                PsiScopeProcessor processor,
                                GrReferenceExpression refExpr,
                                ResolveState state) {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(artifactClass.getProject());
    GlobalSearchScope resolveScope = refExpr.getResolveScope();

    PsiClass builderClass = facade.findClass("org.apache.shiro.grails.AccessControlBuilder", resolveScope);
    if (builderClass == null) {
      builderClass = facade.findClass("org.jsecurity.grails.AccessControlBuilder", resolveScope);

      if (builderClass == null) return true;
    }

    return builderClass.processDeclarations(processor, state, null, refExpr);
  }
}
