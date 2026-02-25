// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.webflow;

import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiVariable;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import java.util.Map;

public final class WebFlowPomDeclarationSearcher extends PomDeclarationSearcher {

  @Override
  public void findDeclarationsAt(@NotNull PsiElement element, int offsetInElement, @NotNull Consumer<? super PomTarget> consumer) {
    if (!(element instanceof GrReferenceExpression)) return;

    PsiElement eMethodCall = element.getParent();
    if (!(eMethodCall instanceof GrMethodCall methodCall)) return;

    if (!WebFlowUtils.isStateDeclaration(methodCall, true)) return;

    String name = WebFlowUtils.getStateNameByStateDeclaration(methodCall);

    Map<String,PsiVariable> webFlowStates = WebFlowUtils.getWebFlowStates(WebFlowUtils.getActionByStateDeclaration(methodCall));

    PsiVariable psiVariable = webFlowStates.get(name);
    if (psiVariable != null) {
      consumer.consume(psiVariable);
    }
  }
}
