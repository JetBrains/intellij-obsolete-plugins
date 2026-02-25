// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.constraints;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMissingMethodContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class GrailsConstraintsGroupClosureMemberProvider extends ClosureMissingMethodContributor {
  private static final Object CONSTRAINT_GROUP_METHOD_MARKER = "Grails:ConstraintGroup:method";

  @Override
  public boolean processMembers(GrClosableBlock closure, PsiScopeProcessor processor, GrReferenceExpression refExpr, ResolveState state) {
    PsiElement methodCall = refExpr.getParent();
    if (methodCall == null) return true;
    if (methodCall.getParent() != closure) return true;

    String nameHint = ResolveUtil.getNameHint(processor);
    if (nameHint == null) return true;

    PsiElement eAssignment = closure.getParent();
    if (!(eAssignment instanceof GrAssignmentExpression)) return true;

    PsiElement eFile = eAssignment.getParent();
    if (!(eFile instanceof GroovyFile)) return true;

    GrExpression lValue = ((GrAssignmentExpression)eAssignment).getLValue();
    if (!(lValue instanceof GrReferenceExpression)) return true;
    if (!lValue.getText().equals(GrailsConstraintsUtil.GRAILS_GORM_DEFAULT_CONSTRAINTS)) return true;

    if (!GrailsUtils.isConfigGroovyFile(eFile)) return true;

    if (refExpr.isQualified()) return true;

    GrLightMethodBuilder res = new GrLightMethodBuilder(eFile.getManager(), nameHint);
    res.addOptionalParameter("constraints", CommonClassNames.JAVA_UTIL_MAP);
    res.setMethodKind(CONSTRAINT_GROUP_METHOD_MARKER);

    return processor.execute(res, state);
  }
}
