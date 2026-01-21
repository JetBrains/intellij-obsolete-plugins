// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.guice.utils.MutationUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public final class MoveProviderBindingToClassIntention extends Intention{
    @Override
    public @NotNull String getText(){
        return GuiceBundle.message("move.provider.binding.to.class.text");
    }

    @Override
    public @NotNull String getFamilyName(){
        return GuiceBundle.message("move.provider.binding.to.class.family.name");
    }

    @Override
    protected @NotNull PsiElementPredicate getElementPredicate(){
        return new MoveProviderBindingToClassPredicate();
    }

    @Override
    protected void processIntention(@NotNull PsiElement element) throws IncorrectOperationException{
        final PsiMethodCallExpression originalCall = (PsiMethodCallExpression) element;
        final PsiClass providerClass = GuiceUtils.findProvidingClassForBinding(originalCall);
        final PsiClass implementedClass = GuiceUtils.findImplementedClassForBinding(originalCall);
        MutationUtils.addAnnotation(implementedClass, "@com.google.inject.ProvidedBy(" + providerClass.getQualifiedName() + ".class)");
        final PsiMethodCallExpression bindingCall = GuiceUtils.findProvidingCallForBinding(originalCall);
        final PsiExpression qualifier = bindingCall.getMethodExpression().getQualifierExpression();

        assert qualifier != null;
        MutationUtils.replaceExpression(qualifier.getText(), bindingCall);
    }
}