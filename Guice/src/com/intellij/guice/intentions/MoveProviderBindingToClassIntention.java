// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.guice.utils.MutationUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UastContextKt;

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
        final UCallExpression uCall = UastContextKt.toUElement(element, UCallExpression.class);
        if (uCall == null) return;
        final PsiClass providerClass = GuiceInjectionUtil.getCallExpressionType(uCall, "toProvider");
        final PsiClass implementedClass = GuiceUtils.findImplementedClassForBinding(uCall);
        MutationUtils.addAnnotation(implementedClass, "@com.google.inject.ProvidedBy(" + providerClass.getQualifiedName() + ".class)");
        final UCallExpression providerCall = GuiceUtils.findCallInChain(uCall, "toProvider");
        final PsiElement providerCallPsi = providerCall.getSourcePsi();
        if (providerCallPsi instanceof PsiMethodCallExpression psiProviderCall) {
            final com.intellij.psi.PsiExpression qualifier = psiProviderCall.getMethodExpression().getQualifierExpression();
            assert qualifier != null;
            MutationUtils.replaceExpression(qualifier.getText(), psiProviderCall);
        }
    }
}