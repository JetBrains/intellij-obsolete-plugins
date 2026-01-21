// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.actions;

import com.intellij.guice.GuiceBundle;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

public class NewGuiceModuleAction extends AbstractNewGuiceClassAction {


    public NewGuiceModuleAction(){
        super(GuiceBundle.messagePointer("new.guice.module.action.name"));
    }

    @Override
    protected void buildDialog(@NotNull Project project, @NotNull PsiDirectory directory,
                               @NotNull CreateFileFromTemplateDialog.Builder builder) {
       builder
          .setTitle(GuiceBundle.message("new.guice.module.action.name"))
          .addKind(GuiceBundle.message("new.guice.module.action.name"),
                   IconManager.getInstance().getPlatformIcon(com.intellij.ui.PlatformIcons.Class), "GuiceNewModule.java");
    }

    @Override
    protected String getActionName(PsiDirectory directory, @NotNull String newName, String templateName) {
        return GuiceBundle.message("new.guice.module.name", directory, newName);
    }

    @Override
    protected @NotNull String getErrorTitle(){
        return GuiceBundle.message("new.guice.module.error");
    }
}
