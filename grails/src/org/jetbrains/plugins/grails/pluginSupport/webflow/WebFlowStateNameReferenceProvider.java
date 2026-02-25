// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.webflow;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.GrailsMethodNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

import java.util.Map;

public class WebFlowStateNameReferenceProvider extends GrailsMethodNamedArgumentReferenceProvider.Contributor.Provider {
  @Override
  public PsiReference[] createRef(@NotNull PsiElement element,
                                  @NotNull GrMethodCall m,
                                  int argumentIndex,
                                  @NotNull GroovyResolveResult resolveResult) {
    final GrClosableBlock stateDefClosure = PsiTreeUtil.getParentOfType(element, GrClosableBlock.class);
    if (stateDefClosure == null) return PsiReference.EMPTY_ARRAY;

    PsiElement parent = stateDefClosure.getParent();
    if (parent instanceof GrArgumentList) parent = parent.getParent();

    if (!(parent instanceof GrMethodCall methodCall)) return PsiReference.EMPTY_ARRAY;

    if (!WebFlowUtils.isStateDeclaration(methodCall, true)) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[] {
      new PsiReferenceBase<>(element, false) {

        private Map<String, PsiVariable> getStates() {
          GrField actionDedField = WebFlowUtils.getActionByStateDeclaration(methodCall);
          return WebFlowUtils.getWebFlowStates(actionDedField);
        }

        @Override
        public PsiElement resolve() {
          return getStates().get(getValue());
        }

        @Override
        public Object @NotNull [] getVariants() {
          return getStates().keySet().toArray();
        }
      }
    };
  }
}
