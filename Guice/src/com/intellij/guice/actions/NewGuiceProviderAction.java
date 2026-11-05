// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.actions;

import com.intellij.guice.GuiceBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
        try{
            return new PsiElement[]{createProviderClass(directory, newName, providedClassName)};
        } catch(IncorrectOperationException e){
            LOGGER.error(e);
            return PsiElement.EMPTY_ARRAY;
        }
    }

    /**
     * Creates {@code <name> implements Provider<providedClass>} from the {@code GuiceNewProvider.java} template in
     * the package of {@code directory}.
     */
    @ApiStatus.Internal
    public static @NotNull PsiClass createProviderClass(@NotNull PsiDirectory directory,
                                                        @NotNull String name,
                                                        @NotNull String providedClass) {
        // All variables of the template are defined here, so no dialog for undefined variables is needed
        // (that mode must not run inside the write action of the create command).
        PsiClass created = JavaDirectoryService.getInstance().createClass(
            directory, name, "GuiceNewProvider.java", false, Map.of("PROVIDED_CLASS", providedClass));
        JavaCodeStyleManager.getInstance(directory.getProject()).shortenClassReferences(created.getContainingFile());
        return created;
    }

    @Override
    protected String getErrorTitle(){
        return GuiceBundle.message("new.guice.provider.error");
    }

    @Override
    protected @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName){
        return GuiceBundle.message("new.guice.provider.name", AbstractNewGuiceClassAction.packageName(directory), newName);
    }
}