package com.intellij.seam.pageflow.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateFileAction;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.javaee.web.WebUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.seam.SeamIcons;
import com.intellij.seam.constants.SeamConstants;
import com.intellij.seam.facet.SeamFacet;
import com.intellij.seam.resources.SeamBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

public final class CreatePageflowFileAction extends CreateFileAction {
  public CreatePageflowFileAction() {
    super(SeamBundle.messagePointer("seam.pageflow.new.file"), SeamBundle.messagePointer("create.new.seam.pageflow.file"), SeamIcons.Seam);

  }

  @Override
  protected boolean isAvailable(final DataContext dataContext) {
    if (!super.isAvailable(dataContext)) {
      return false;
    }
    final Module module = PlatformCoreDataKeys.MODULE.getData(dataContext);
    return module != null && SeamFacet.getInstance(module) != null && isDirectoryAccepted(module, dataContext);
  }

  private static boolean isDirectoryAccepted(final Module module, final DataContext context) {

    final IdeView view = LangDataKeys.IDE_VIEW.getData(context);

    if (view != null) {
      for (PsiDirectory dir : view.getDirectories()) {
        if (WebUtil.isInsideWebRoots(dir.getVirtualFile(), module.getProject())) return true;
      }
    }

    return false;

  }

  @Override
  protected PsiElement @NotNull [] invokeDialog(final @NotNull Project project, @NotNull PsiDirectory directory) {
    MyInputValidator validator = new MyInputValidator(project, directory);
    Messages.showInputDialog(project, IdeBundle.message("prompt.enter.new.file.name"), IdeBundle.message("title.new.file"),
                             Messages.getQuestionIcon(), null, validator);
    return validator.getCreatedElements();
  }

  @Override
  protected PsiElement @NotNull [] create(@NotNull final String newName, final @NotNull PsiDirectory directory) throws Exception {

    @NonNls final String fileName = FileUtilRt.getExtension(newName).length() == 0 ? newName + ".xml" : newName;

    final Properties properties = new Properties();
    properties.setProperty("PAGEFLOW_NAME", newName);

    final PsiElement psiElement = FileTemplateUtil.createFromTemplate(getTemplate(directory.getProject()), fileName, properties, directory);

    return new PsiElement[]{psiElement};
  }

   @NotNull
   private static FileTemplate getTemplate(Project project) {
    return FileTemplateManager.getInstance(project).getJ2eeTemplate(SeamConstants.FILE_TEMPLATE_NAME_PAGEFLOW_2_0);
  }
}