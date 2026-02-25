// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMissingMethodContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

public class GrailsEnvironmentClosureMemberContributor extends ClosureMissingMethodContributor {

  private static final Object ENVIRONMENT_NAME_METHOD_KIND = "grails:environment:name";

  @Override
  public boolean processMembers(GrClosableBlock closure, PsiScopeProcessor processor, GrReferenceExpression ref, ResolveState state) {
    if (ref.isQualified()) return true;

    PsiElement parent = ref.getParent();
    if (parent instanceof GrMethodCall && ((GrMethodCall)parent).getInvokedExpression() == ref) parent = parent.getParent();

    if (parent != closure) return true;

    String nameHint = ResolveUtil.getNameHint(processor);

    if (nameHint == null) {
      for (String envName : GrailsUtils.ENVIRONMENT_LIST) {
        GrLightMethodBuilder envMethod = new GrLightMethodBuilder(closure.getManager(), envName);
        envMethod.setMethodKind(ENVIRONMENT_NAME_METHOD_KIND);
        envMethod.addParameter("closure", GroovyCommonClassNames.GROOVY_LANG_CLOSURE);
        if (!processor.execute(envMethod, state)) return false;
      }
    }
    else if (GrailsUtils.ENVIRONMENT_LIST.contains(nameHint)) {
      GrLightMethodBuilder envMethod = new GrLightMethodBuilder(closure.getManager(), nameHint);
      envMethod.setMethodKind(ENVIRONMENT_NAME_METHOD_KIND);
      envMethod.addParameter("closure", GroovyCommonClassNames.GROOVY_LANG_CLOSURE);
      if (!processor.execute(envMethod, state)) return false;
    }

    return true;
  }

}
