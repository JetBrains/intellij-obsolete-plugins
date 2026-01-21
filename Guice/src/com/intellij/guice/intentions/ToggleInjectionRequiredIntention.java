// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.utils.MutationUtils;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public final class ToggleInjectionRequiredIntention extends MutablyNamedIntention{
    @Override
    protected String getTextForElement(PsiElement element){
        final PsiAnnotation annotation = (PsiAnnotation) element;
        final PsiAnnotationMemberValue value = annotation.findAttributeValue("optional");
        if(value == null){
            return GuiceBundle.message("make.injection.optional");
        }
        if(value instanceof PsiLiteralExpression){
            if(value.getText().equals("false")){
                return GuiceBundle.message("make.injection.optional");
            } else{
                return GuiceBundle.message("make.injection.mandatory");
            }
        }
        return GuiceBundle.message("toggle.required");
    }

    @Override
    public @NotNull String getFamilyName(){
        return GuiceBundle.message("toggle.injection.required.family.name");
    }

    @Override
    protected @NotNull PsiElementPredicate getElementPredicate(){
        return new ToggleInjectionRequiredPredicate();
    }

    @Override
    protected void processIntention(@NotNull PsiElement element) throws IncorrectOperationException{
        final PsiAnnotation annotation = (PsiAnnotation) element;
        final PsiAnnotationMemberValue value = annotation.findAttributeValue("optional");
        if(value == null){
            MutationUtils.replaceAnnotation(annotation, "@com.google.inject.Inject(optional = true)");
        } else if(value instanceof PsiLiteralExpression){
            if(value.getText().equals("false")){
                MutationUtils.replaceAnnotation(annotation, "@com.google.inject.Inject(optional = true)");
            } else{
                MutationUtils.replaceAnnotation(annotation, "@com.google.inject.Inject");
            }
        } else{
            MutationUtils.negateExpression((PsiExpression) value);
        }
    }
}