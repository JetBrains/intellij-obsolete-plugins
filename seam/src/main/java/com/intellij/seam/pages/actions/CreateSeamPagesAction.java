package com.intellij.seam.pages.actions;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.seam.actions.BaseCreateSeamAction;
import com.intellij.seam.actions.CreateSeamComponentsAction;
import com.intellij.seam.constants.SeamConstants;
import com.intellij.seam.resources.SeamBundle;
import com.intellij.seam.SeamIcons;
import org.jetbrains.annotations.NotNull;


public class CreateSeamPagesAction extends BaseCreateSeamAction {
  public static final Logger LOG = Logger.getInstance(CreateSeamComponentsAction.class.getName());

  public CreateSeamPagesAction() {
    super(SeamBundle.messagePointer("seam.pages.new.file"), SeamBundle.messagePointer("create.new.seam.pages.file"), SeamIcons.Seam);
  }

  @Override
  @NotNull
  protected FileTemplate getTemplate(final Module module) {
    return FileTemplateManager.getInstance(module.getProject()).getJ2eeTemplate(SeamConstants.FILE_TEMPLATE_NAME_PAGES_2_0);
  }

  @Override
  @NotNull
  protected String getFileName() {
    return SeamConstants.SEAM_PAGES_FILENAME;
  }

   @Override
   protected boolean isAllowedInSourceDir() {
    return false;
  }
}
