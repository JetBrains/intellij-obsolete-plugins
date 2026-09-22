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

public final class MoveBindingToClassIntention extends Intention{
    @Override
    public @NotNull String getText(){
        return GuiceBundle.message("move.binding.to.class.text");
    }

    @Override
    public @NotNull String getFamilyName(){
        return GuiceBundle.message("move.binding.to.class.family.name");
    }

    @Override
    protected @NotNull PsiElementPredicate getElementPredicate(){
        return new MoveBindingToClassPredicate();
    }

    @Override
    protected void processIntention(@NotNull PsiElement element) throws IncorrectOperationException{
        final UCallExpression uCall = UastContextKt.toUElement(element, UCallExpression.class);
        if (uCall == null) return;
        final PsiClass implmentingClass = GuiceInjectionUtil.getCallExpressionType(uCall, "to");
        final PsiClass implementedClass = GuiceUtils.findImplementedClassForBinding(uCall);
        MutationUtils.addAnnotation(implementedClass, "@com.google.inject.ImplementedBy(" + implmentingClass.getQualifiedName() + ".class)");
        final UCallExpression bindingCall = GuiceUtils.findCallInChain(uCall, "to");
        final PsiElement bindingCallPsi = bindingCall.getSourcePsi();
        if (bindingCallPsi instanceof PsiMethodCallExpression psiBindingCall) {
            final com.intellij.psi.PsiExpression qualifier = psiBindingCall.getMethodExpression().getQualifierExpression();
            assert qualifier != null;
            MutationUtils.replaceExpression(qualifier.getText(), psiBindingCall);
        }
    }
}