package com.intellij.vaadin.actions;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtil;
import com.intellij.vaadin.VaadinBundle;
import com.intellij.vaadin.framework.VaadinVersion;
import com.intellij.vaadin.framework.VaadinVersionUtil;
import org.jetbrains.annotations.NotNull;

public class CreateCustomComponentAction extends VaadinCreateElementActionBase {
  public CreateCustomComponentAction() {
    super(VaadinBundle.message("action.vaadin.custom.component.text"),
          VaadinBundle.message("action.create.new.vaadin.custom.component.description"));
  }

  @Override
  protected PsiElement @NotNull [] create(@NotNull String newName, @NotNull PsiDirectory directory) throws Exception {
    Module module = ModuleUtilCore.findModuleForFile(directory.getVirtualFile(), directory.getProject());
    if (module == null) return PsiElement.EMPTY_ARRAY;
    PsiUtil.checkIsIdentifier(directory.getManager(), newName);

    VaadinVersion version = VaadinVersionUtil.getVaadinVersion(module);
    PsiClass aClass =
      JavaDirectoryService.getInstance().createClass(directory, newName, version.getTemplateNames().getCustomComponent());
    return new PsiElement[]{aClass};
  }

  @Override
  protected PsiElement @NotNull [] invokeDialog(@NotNull Project project, @NotNull PsiDirectory directory) {
    MyInputValidator validator = new MyInputValidator(project, directory);
    Messages.showInputDialog(project, VaadinBundle.message("dialog.message.enter.new.class.name"),
                             VaadinBundle.message("dialog.title.new.vaadin.custom.component"), Messages.getQuestionIcon(), "", validator);
    return validator.getCreatedElements();
  }

  @Override
  protected String getErrorTitle() {
    return VaadinBundle.message("dialog.title.cannot.create.custom.component");
  }

  @Override
  protected @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName) {
    return VaadinBundle.message("command.name.creating.vaadin.custom.component", newName);
  }
}
