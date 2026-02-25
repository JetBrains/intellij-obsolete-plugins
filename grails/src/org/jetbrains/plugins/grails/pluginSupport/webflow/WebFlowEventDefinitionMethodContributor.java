// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.webflow;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMissingMethodContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class WebFlowEventDefinitionMethodContributor extends ClosureMissingMethodContributor {
  @Override
  public boolean processMembers(GrClosableBlock closure, PsiScopeProcessor processor, GrReferenceExpression refExpr, ResolveState state) {
    PsiElement eActionMethodCall = closure.getParent();
    if (eActionMethodCall instanceof GrArgumentList) eActionMethodCall = eActionMethodCall.getParent();

    if (!(eActionMethodCall instanceof GrMethodCall actionMethodCall)) return true;

    String nameHint = ResolveUtil.getNameHint(processor);
    if (nameHint == null) return true;

    GrExpression ie = actionMethodCall.getInvokedExpression();
    if (!PsiUtil.isReferenceWithoutQualifier(ie, "action") && !PsiUtil.isReferenceWithoutQualifier(ie, "on")) return true;

    GrClosableBlock stateDefClosure = PsiTreeUtil.getParentOfType(actionMethodCall, GrClosableBlock.class);
    if (stateDefClosure == null) return true;

    PsiElement stateDefMethodCall = stateDefClosure.getParent();
    if (stateDefMethodCall instanceof GrArgumentList) stateDefMethodCall = stateDefMethodCall.getParent();

    if (!(stateDefMethodCall instanceof GrMethodCall)) return true;

    if (!WebFlowUtils.isStateDeclaration((GrMethodCall)stateDefMethodCall, true)) return true;

    PsiElement parent = refExpr.getParent();
    if (!(parent instanceof GrMethodCall)) return true;
    GrArgumentList al = ((GrMethodCall)parent).getArgumentList();
    if (al.getAllArguments().length > 1) return true;

    GrLightMethodBuilder method = new GrLightMethodBuilder(stateDefClosure.getManager(), nameHint);
    method.setReturnType("org.springframework.webflow.execution.Event", stateDefClosure.getResolveScope());
    method.addOptionalParameter("args", CommonClassNames.JAVA_LANG_OBJECT);
    return processor.execute(method, state);
  }
}
