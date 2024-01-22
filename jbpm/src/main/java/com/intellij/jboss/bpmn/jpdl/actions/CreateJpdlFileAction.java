package com.intellij.jboss.bpmn.jpdl.actions;

import com.intellij.ide.actions.CreateFileAction;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.providers.JpdlBeansTemplatesFactory;
import com.intellij.jboss.bpmn.jpdl.resources.messages.JpdlBundle;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

public class CreateJpdlFileAction extends CreateFileAction {
  private CreateJpdlDialog myDialog;

  public CreateJpdlFileAction() {
    super(JpdlBundle.messagePointer("create.jpdl.action.title"),
          JpdlBundle.messagePointer("create.jpdl.process.dialog.title"), () -> JbossJbpmIcons.Jboss);
  }

  @Override
  protected boolean isAvailable(DataContext dataContext) {
    if (!super.isAvailable(dataContext)) {
      return false;
    }
    final Module module = PlatformCoreDataKeys.MODULE.getData(dataContext);
    return module != null && JavaPsiFacade.getInstance(module.getProject()).findPackage("org.jbpm.jpdl") != null;
  }

  @Override
  protected PsiElement @NotNull [] invokeDialog(@NotNull Project project, @NotNull PsiDirectory directory) {
    MyInputValidator validator = new MyInputValidator(project, directory);
    myDialog = new CreateJpdlDialog(project, validator);
    myDialog.show();
    return validator.getCreatedElements();
  }

  @Override
  protected PsiElement @NotNull [] create(@NotNull String newName, @NotNull PsiDirectory directory) throws Exception {
    final String processName = myDialog.getProcessName();//todo
    FileTemplateManager manager = FileTemplateManager.getInstance(directory.getProject());
    FileTemplate template = manager.getJ2eeTemplate(JpdlBeansTemplatesFactory.PROCESS_4_4_JPDL_XML);
    final Properties properties = new Properties();
    properties.setProperty("NAME", processName);
    return new PsiElement[]{FileTemplateUtil.createFromTemplate(template, newName, properties, directory)};
  }
}