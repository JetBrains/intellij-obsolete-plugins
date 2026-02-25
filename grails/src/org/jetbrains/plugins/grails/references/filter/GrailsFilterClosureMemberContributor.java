// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.filter;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrImplicitVariableImpl;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class GrailsFilterClosureMemberContributor extends ClosureMemberContributor {
  private static final String[] PROPERTIES = {"before", "after", "afterView"};

  @Override
  public void processMembers(@NotNull GrClosableBlock closure,
                             @NotNull PsiScopeProcessor processor,
                             @NotNull PsiElement place,
                             @NotNull ResolveState state) {
    if (!(place instanceof GrReferenceExpression refExpr)) return;
    if (refExpr.isQualified()) return;

    if (ResolveUtil.shouldProcessProperties(processor.getHint(ElementClassHint.KEY))) {
      String name = ResolveUtil.getNameHint(processor);
      if (name != null && !ArrayUtil.contains(name, PROPERTIES)) return;

      PsiElement eMethodCall = closure.getParent();

      if (!GrailsFilterUtil.isFilterDefinitionMethod(eMethodCall)) return;

      PsiManager manager = eMethodCall.getManager();

      if (name == null) {
        for (String property : PROPERTIES) {
          PsiVariable var = new GrImplicitVariableImpl(manager, property, CommonClassNames.JAVA_LANG_OBJECT, refExpr);
          if (!processor.execute(var, state)) return;
        }
      }
      else {
        PsiVariable var = new GrImplicitVariableImpl(manager, name, CommonClassNames.JAVA_LANG_OBJECT, refExpr);
        if (!processor.execute(var, state)) //noinspection UnnecessaryReturnStatement
          return;
      }
    }
  }

}
