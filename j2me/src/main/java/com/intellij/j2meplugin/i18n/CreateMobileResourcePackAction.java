/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.i18n;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.CreateElementActionBase;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.module.J2MEModuleType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CreateMobileResourcePackAction extends CreateElementActionBase {
  @NonNls public static final String MOBILE_RESOURCE_BUNDLE = "MobileResourceBundle";

  public CreateMobileResourcePackAction() {
    super(J2MEBundle.message("create.mobile.resource.bundle.action.text"),
          J2MEBundle.message("create.mobile.resource.bundle.action.text"),
          StdFileTypes.JAVA.getIcon());
  }


  @Override
  @NotNull
  public PsiElement[] invokeDialog(Project project, PsiDirectory directory) {
    CreateElementActionBase.MyInputValidator validator = new CreateElementActionBase.MyInputValidator(project, directory);
    Messages.showInputDialog(project, IdeBundle.message("prompt.enter.new.class.name"),
                             IdeBundle.message("title.new.class"), Messages.getQuestionIcon(), "", validator);
    return validator.getCreatedElements();
  }

  @Override
  @NotNull
  protected PsiElement[] create(@NotNull String newName, PsiDirectory directory) throws Exception {
    final PsiClass psiClass = JavaDirectoryService.getInstance().createClass(directory, newName, MOBILE_RESOURCE_BUNDLE);
    final PsiFile propertiesFile = directory.createFile(newName + "." + StdFileTypes.PROPERTIES.getDefaultExtension());
    final Module module = ModuleUtil.findModuleForPsiElement(directory);
    if (module != null) {
      /*final ResourceBundleManager[] managers = module.getProject().getExtensions(ResourceBundleManager.RESOURCE_BUNDLE_MANAGER);
      for (ResourceBundleManager manager : managers) {
        if (manager instanceof MobileResourceBundleManager) {
          ((MobileResourceBundleManager)manager).registerResourceBundle(psiClass);
          break;
        }
      }*/
      ResourceBeansContainer.getInstance(module.getProject()).registerResourceBundle(psiClass);
    }
    return new PsiElement[]{psiClass, propertiesFile};
  }

  @Override
  protected String getErrorTitle() {
    return J2MEBundle.message("cannot.create.mobile.resource.bundle.error.title");
  }

  @Override
  protected String getCommandName() {
    return J2MEBundle.message("create.mobile.resource.bundle.action.name");
  }

  @Override
  protected String getActionName(PsiDirectory directory, String newName) {
    return J2MEBundle.message("create.mobile.resource.bundle.action.name");
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    super.update(e);
    final Module module = e.getData(LangDataKeys.MODULE);
    if (module == null || !(ModuleType.get(module) instanceof J2MEModuleType)) {
      e.getPresentation().setVisible(false);
    }
  }
}
