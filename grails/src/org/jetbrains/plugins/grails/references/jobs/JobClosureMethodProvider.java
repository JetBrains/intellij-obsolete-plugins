// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.jobs;

import com.intellij.psi.PsiClass;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.GrailsClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

public final class JobClosureMethodProvider implements GrailsClosureMemberContributor.MethodProvider {

  private static final String CLASS_SOURCE = """
    class JobTriggerElements {  private void simple(Map arg) { def z = arg.name + arg.startDelay + arg.repeatInterval + arg.repeatCount}
      private void cron(Map arg) { def z = arg.name + arg.startDelay + arg.cronExpression}
      private void custom(Map arg) { def z = arg.triggerClass}
    }""";

  @Override
  public boolean processMembers(@NotNull GrClosableBlock closure,
                                PsiClass artifactClass,
                                PsiScopeProcessor processor,
                                GrReferenceExpression refExpr,
                                ResolveState state) {
    return DynamicMemberUtils.process(processor, false, refExpr, CLASS_SOURCE);
  }
}
