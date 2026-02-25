// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMissingMethodContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.List;

public class GrailsPluginConfigMethodContributor extends ClosureMissingMethodContributor {

  public static final List<String> SCOPES = List.of("build", "compile", "runtime", "test", "provided");

  public static final String METHOD_KIND = "GrailsPluginConfigMethodContributor:pluginCreation";

  @Override
  public boolean processMembers(GrClosableBlock closure, PsiScopeProcessor processor, GrReferenceExpression refExpr, ResolveState state) {
    String nameHint = ResolveUtil.getNameHint(processor);

    if (nameHint != null && !SCOPES.contains(nameHint)) return true;

    if (nameHint == null) {
      for (String scope : SCOPES) {
        if (!processor.execute(createPluginBuildMethod(closure.getManager(), scope), state)) return false;
      }
    }
    else {
      if (!processor.execute(createPluginBuildMethod(closure.getManager(), nameHint), state)) return false;
    }

    return false;
  }

  private static PsiMethod createPluginBuildMethod(PsiManager psiManager, String scopeName) {
    GrLightMethodBuilder res = new GrLightMethodBuilder(psiManager, scopeName);
    res.addParameter("pluginCoordinates", CommonClassNames.JAVA_LANG_STRING);
    res.setMethodKind(METHOD_KIND);
    return res;
  }
}
