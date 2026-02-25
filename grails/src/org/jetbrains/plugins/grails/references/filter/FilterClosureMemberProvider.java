// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.filter;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.GrailsClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

public class FilterClosureMemberProvider implements GrailsClosureMemberContributor.MethodProvider {

  @Override
  public boolean processMembers(@NotNull GrClosableBlock closure,
                                PsiClass artifactClass,
                                PsiScopeProcessor processor,
                                GrReferenceExpression refExpr,
                                ResolveState state) {
    if (refExpr.isQualified()) return true;
    
    PsiElement eMethodCall = refExpr.getParent();
    if (!(eMethodCall instanceof GrMethodCall methodCall)) return true;

    GrExpression[] args = PsiUtil.getAllArguments(methodCall);
    if (args.length == 0) return true;

    int i = 0;
    if (args[i] == null || args[i] instanceof GrListOrMap) i++; // Skip first parameter if it's a map or named argument exists (args[0] == null, see PsiUtil.getAllArguments)
    
    if (args.length != i + 1) return true;
    if (!(args[i] instanceof GrClosableBlock)) return true;

    String name = ResolveUtil.getNameHint(processor);

    if (name == null) return true;

    GrLightMethodBuilder method = new GrLightMethodBuilder(closure.getManager(), name);
    method.setMethodKind(FilterClosureMemberProvider.class);
    method.addOptionalParameter("args", CommonClassNames.JAVA_UTIL_MAP);
    method.addParameter("closure", GroovyCommonClassNames.GROOVY_LANG_CLOSURE);
    method.setReturnType(PsiTypes.voidType());

    return processor.execute(method, state);
  }

  public static boolean isFilterDefinitionMethod(@Nullable PsiMethod method) {
    return GrLightMethodBuilder.checkKind(method, FilterClosureMemberProvider.class);
  }

}
