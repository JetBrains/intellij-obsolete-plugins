// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class BaseInspectionVisitor extends JavaElementVisitor{

    private BaseInspection inspection = null;
    private boolean onTheFly = false;
    private final List<ProblemDescriptor> errors = null;
    private ProblemsHolder holder = null;
    private InspectionManager inspectionManager;

    public void setInspection(BaseInspection inspection){
        this.inspection = inspection;
    }

    public void setOnTheFly(boolean onTheFly){
        this.onTheFly = onTheFly;
    }

    protected void registerMethodCallError(PsiMethodCallExpression expression,
                                           Object... infos){
        final PsiReferenceExpression methodExpression =
                expression.getMethodExpression();
        final PsiElement nameToken = methodExpression.getReferenceNameElement();
        registerError(nameToken != null?nameToken:expression, infos);
    }

    protected void registerStatementError(PsiStatement statement,
                                          Object... infos){
        final PsiElement statementToken = statement.getFirstChild();
        registerError(statementToken != null?statementToken:statement, infos);
    }

    protected void registerClassError(PsiClass aClass, Object... infos){
        final PsiElement nameIdentifier;
        if(aClass instanceof PsiAnonymousClass anonymousClass){
          nameIdentifier = anonymousClass.getBaseClassReference();
        } else{
            nameIdentifier = aClass.getNameIdentifier();
        }
        registerError(nameIdentifier != null?nameIdentifier:aClass.getContainingFile(), infos);
    }

    protected void registerMethodError(PsiMethod method, Object... infos){
        final PsiElement nameIdentifier = method.getNameIdentifier();
        registerError(nameIdentifier != null?nameIdentifier:method.getContainingFile(), infos);
    }

    protected void registerVariableError(PsiVariable variable, Object... infos){
        final PsiElement nameIdentifier = variable.getNameIdentifier();
        registerError(nameIdentifier != null?nameIdentifier:variable.getContainingFile(), infos);
    }

    protected void registerTypeParameterError(PsiTypeParameter param,
                                              Object... infos){
        final PsiElement nameIdentifier = param.getNameIdentifier();
        registerError(nameIdentifier, infos);
    }

    protected void registerFieldError(PsiField field, Object... infos){
        final PsiElement nameIdentifier = field.getNameIdentifier();
        registerError(nameIdentifier, infos);
    }

    protected void registerModifierError(String modifier,
                                         PsiModifierListOwner parameter,
                                         Object... infos){
        final PsiModifierList modifiers = parameter.getModifierList();
        if(modifiers == null){
            return;
        }
        final PsiElement[] children = modifiers.getChildren();
        for(final PsiElement child : children){
            final String text = child.getText();
            if(modifier.equals(text)){
                registerError(child, infos);
            }
        }
    }

    protected void registerError(@NotNull PsiElement location, Object... infos){
        final LocalQuickFix[] fixes = createFixes(location, infos);
        final String description = inspection.buildErrorString(infos);
        holder.registerProblem(location, description, fixes);
    }

    private @NotNull LocalQuickFix @Nullable [] createFixes(PsiElement location, Object[] infos){
        if(!onTheFly && inspection.buildQuickFixesOnlyForOnTheFlyErrors()){
            return null;
        }

        final LocalQuickFix fix = inspection.buildFix(location, infos);
        if(fix == null){
            return null;
        }
        return new LocalQuickFix[]{fix};
    }

    @Override
    public void visitReferenceExpression(@NotNull PsiReferenceExpression expression){
        visitExpression(expression);
    }

    @Override
    public void visitWhiteSpace(@NotNull PsiWhiteSpace space){
        // none of our inspections need to do anything with white space,
        // so this is a performance optimization
    }

    public void setProblemsHolder(ProblemsHolder holder){
        this.holder = holder;
    }

    public void initialize(){
    }

    public void setInspectionManager(InspectionManager inspectionManager){
        this.inspectionManager = inspectionManager;
    }
}
