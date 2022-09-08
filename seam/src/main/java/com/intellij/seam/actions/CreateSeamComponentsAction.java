package com.intellij.seam.actions;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.seam.SeamIcons;
import com.intellij.seam.constants.SeamConstants;
import com.intellij.seam.resources.SeamBundle;
import com.intellij.seam.utils.SeamCommonUtils;
import org.jetbrains.annotations.NotNull;

public class CreateSeamComponentsAction extends BaseCreateSeamAction {
  public static final Logger LOG = Logger.getInstance(CreateSeamComponentsAction.class.getName());

  public CreateSeamComponentsAction() {
    super(SeamBundle.messagePointer("seam.components.new.file"), SeamBundle.messagePointer("create.new.seam.components.file"), SeamIcons.Seam);
  }

  @Override
  @NotNull
  protected FileTemplate getTemplate(final Module module) {
    return SeamCommonUtils.chooseTemplate(module);
  }

  @Override
  @NotNull
  protected String getFileName() {
    return SeamConstants.SEAM_CONFIG_FILENAME;
  }
}

