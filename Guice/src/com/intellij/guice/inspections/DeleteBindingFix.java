// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.utils.MutationUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

class DeleteBindingFix implements LocalQuickFix{
    private static final Logger LOGGER = Logger.getInstance("RedundantToProviderBindingInspection");

    @Override
    public @NotNull String getName(){
        return GuiceBundle.message("delete.binding");
    }

    @Override
    public @NotNull String getFamilyName(){
        return "";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor){
        final PsiMethodCallExpression call =
                PsiTreeUtil.getParentOfType(descriptor.getPsiElement(), PsiMethodCallExpression.class);
        assert call != null;
        final PsiExpression qualifier = call.getMethodExpression().getQualifierExpression();
        try{
            assert qualifier != null;
            MutationUtils.replaceExpression(qualifier.getText(), call);
        } catch(IncorrectOperationException e){
            LOGGER.error(e);
        }
    }
}
