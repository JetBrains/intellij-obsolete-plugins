// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.actions;

import com.intellij.guice.GuiceBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class NewGuiceProviderAction extends GeneratePluginClassAction{

    private static final Logger LOGGER = Logger.getInstance("NewGuiceProviderAction");
    private String providedClassName = null;

    public NewGuiceProviderAction(){
        super(GuiceBundle.messagePointer("new.guice.provider.action.name"),
              GuiceBundle.messagePointer("new.guice.provider.action.name"),
              null);
    }

    @Override
    protected PsiElement[] invokeDialogImpl(Project project, PsiDirectory directory) {
        final ProviderDialog dialog = new ProviderDialog(project);
        if (dialog.showAndGet()) {
            final String providerName = dialog.getProviderName();
            providedClassName = dialog.getProvidedClass();
            final MyInputValidator validator = new MyInputValidator(project, directory);
            validator.canClose(providerName);
            return validator.getCreatedElements();
        }
        return PsiElement.EMPTY_ARRAY;
    }

    @Override
    protected PsiElement @NotNull [] create(@NotNull String newName, @NotNull PsiDirectory directory){
        final Project project = directory.getProject();
        final PsiFileFactory elementFactory = PsiFileFactory.getInstance(project);
        final GuiceProviderBuilder builder = new GuiceProviderBuilder();
        builder.setClassName(newName);
        builder.setProvidedClassName(providedClassName);

        final String beanClassString;
        try{
            beanClassString = builder.buildProviderClass(project);
        } catch(IOException e){
            LOGGER.error(e);
            return PsiElement.EMPTY_ARRAY;
        }
        try{
            final PsiFile newFile = elementFactory.createFileFromText(newName + ".java", beanClassString);
            final JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
            final PsiElement shortenedFile = codeStyleManager.shortenClassReferences(newFile);
            final PsiElement reformattedFile = CodeStyleManager.getInstance(project).reformat(shortenedFile);
            directory.add(reformattedFile);
            return new PsiElement[]{reformattedFile};
        } catch(IncorrectOperationException e){
            LOGGER.error(e);
            return PsiElement.EMPTY_ARRAY;
        }
    }

    @Override
    protected String getErrorTitle(){
        return GuiceBundle.message("new.guice.provider.error");
    }

    @Override
    protected @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName){
        return GuiceBundle.message("new.guice.provider.name", directory, newName);
    }
}