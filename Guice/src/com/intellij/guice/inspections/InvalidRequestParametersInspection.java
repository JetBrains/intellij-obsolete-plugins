// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.guice.GuiceBundle;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public final class InvalidRequestParametersInspection extends BaseInspection{

    @Override
    protected @NotNull String buildErrorString(Object... infos){
        return GuiceBundle.message("invalid.request.parameters.problem.descriptor");
    }

    @Override
    public BaseInspectionVisitor buildVisitor(){
        return new Visitor();
    }

    private static class Visitor extends BaseInspectionVisitor{
        @Override
        public void visitAnnotation(@NotNull PsiAnnotation annotation){
            super.visitAnnotation(annotation);
            if(!"com.google.inject.servlet.RequestParameters".equals(annotation.getQualifiedName())){
                return;
            }
            final PsiVariable variable = PsiTreeUtil.getParentOfType(annotation, PsiVariable.class);
            if(variable == null){
                return;
            }
            final PsiType type = variable.getType();
            String typeText = type.getCanonicalText();
            typeText = typeText.replaceAll(" ", "");
            if(typeText.equals("java.util.Map<java.lang.String,java.lang.String[]>")){
                return;
            }
            registerError(annotation);
        }
    }
}