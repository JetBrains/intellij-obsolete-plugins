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

public final class MoveBindingScopeToClassIntention extends Intention{
    @Override
    public @NotNull String getText(){
        return GuiceBundle.message("move.binding.scope.to.class.text");
    }

    @Override
    public @NotNull String getFamilyName(){
        return GuiceBundle.message("move.binding.scope.to.class.family.name");
    }

    @Override
    protected @NotNull PsiElementPredicate getElementPredicate(){
        return new MoveBindingScopeToClassPredicate();
    }

    @Override
    protected void processIntention(@NotNull PsiElement element) throws IncorrectOperationException{
        final PsiMethodCallExpression originalCall = (PsiMethodCallExpression) element;
        final PsiClass bindingClass = GuiceUtils.findImplementingClassForBinding(originalCall);
        final PsiMethodCallExpression scopeCall = GuiceUtils.findScopeCallForBinding(originalCall);
        final PsiExpression arg = scopeCall.getArgumentList().getExpressions()[0];
        final String scopeAnnotation = GuiceUtils.getScopeAnnotationForScopeExpression(arg);
        MutationUtils.addAnnotation(bindingClass, "@" + scopeAnnotation);
        final PsiExpression qualifier = scopeCall.getMethodExpression().getQualifierExpression();

        assert qualifier != null;
        MutationUtils.replaceExpression(qualifier.getText(), scopeCall);
    }
}
