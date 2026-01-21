// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.AnnotationUtils;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public final class InvalidProvidedByInspection extends BaseInspection{

    @Override
    protected @NotNull String buildErrorString(Object... infos){
        return GuiceBundle.message("invalid.provided.by.problem.descriptor");
    }

    @Override
    public BaseInspectionVisitor buildVisitor(){
        return new Visitor();
    }

    private static class Visitor extends BaseInspectionVisitor{
        @Override
        public void visitAnnotation(@NotNull PsiAnnotation annotation){
            super.visitAnnotation(annotation);
            final String qualifiedName = annotation.getQualifiedName();
            if(!GuiceAnnotations.PROVIDED_BY.equals(qualifiedName)){
                return;
            }
            final PsiClass containingClass = PsiTreeUtil.getParentOfType(annotation, PsiClass.class);
            if(containingClass == null){
                return;
            }
            final PsiElement defaultValue = AnnotationUtils.findDefaultValue(annotation);
            if(defaultValue == null){
                return;
            }
            if(!(defaultValue instanceof PsiClassObjectAccessExpression)){
                return;
            }
            final PsiTypeElement classTypeElement = ((PsiClassObjectAccessExpression) defaultValue).getOperand();
            final PsiType classType = classTypeElement.getType();
            if(!(classType instanceof PsiClassType)){
                return;
            }
            final PsiClass referentClass = ((PsiClassType) classType).resolve();
            if(referentClass == null){
                return;
            }
            if(GuiceUtils.provides(referentClass, containingClass))
            {
                return;
            }
            registerError(classTypeElement);
        }
    }

}