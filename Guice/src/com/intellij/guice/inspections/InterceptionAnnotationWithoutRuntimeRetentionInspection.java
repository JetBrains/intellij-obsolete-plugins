// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.GuiceBundle;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public final class InterceptionAnnotationWithoutRuntimeRetentionInspection extends BaseInspection{

    @Override
    protected @NotNull String buildErrorString(Object... infos){
        return GuiceBundle.message("interception.annotation.without.runtime.retention.problem.descriptor");
    }

    @Override
    public BaseInspectionVisitor buildVisitor(){
        return new Visitor();
    }

    private static class Visitor extends BaseInspectionVisitor{
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression){
            super.visitMethodCallExpression(expression);
            final PsiReferenceExpression methodExpression = expression.getMethodExpression();
            final String name = methodExpression.getReferenceName();
            if(!"annotatedWith".equals(name)){
                return;
            }
            final PsiExpression[] args = expression.getArgumentList().getExpressions();
            if(args.length != 1){
                return;
            }
            if(!(args[0] instanceof PsiClassObjectAccessExpression arg)){
                return;
            }
            final PsiMethod method = expression.resolveMethod();
            if(method == null){
                return;
            }
            final PsiClass containingClass = method.getContainingClass();
            if(!"com.google.inject.matcher.Matchers".equals(containingClass.getQualifiedName())){
                return;
            }
          final PsiTypeElement operandType = arg.getOperand();
            final PsiClassType operandAnnotationType = (PsiClassType) operandType.getType();
            final PsiClass operantAnnotation = operandAnnotationType.resolve();
            final PsiAnnotation retentionAnnotation =
                    AnnotationUtil.findAnnotation(operantAnnotation, Collections.singleton("java.lang.annotation.Retention"));
            if(retentionAnnotation == null || !retentionAnnotation.getText().contains("RUNTIME")){
                registerError(operandType);
            }
        }
    }
}