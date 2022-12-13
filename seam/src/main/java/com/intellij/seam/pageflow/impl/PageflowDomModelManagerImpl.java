package com.intellij.seam.pageflow.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.pageflow.model.xml.PageflowDomModelManager;
import com.intellij.seam.pageflow.model.xml.PageflowModel;
import com.intellij.seam.pageflow.model.xml.PageflowModelFactory;
import com.intellij.seam.pageflow.model.xml.pageflow.PageflowDefinition;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class PageflowDomModelManagerImpl extends PageflowDomModelManager {
  private final PageflowModelFactory myModelFactory;
  private final DomManager myDomManager;

  PageflowDomModelManagerImpl(@NotNull Project project) {
    myDomManager = DomManager.getDomManager(project);
    myModelFactory = new PageflowModelFactory(project);
  }

  @Override
  public boolean isPageflow(@NotNull final XmlFile file) {
    return myDomManager.getFileElement(file, PageflowDefinition.class) != null;
  }

  @Override
  public PageflowModel getPageflowModel(@NotNull final XmlFile file) {
    return myModelFactory.getModelByConfigFile(file);
  }

  @Override
  public List<PageflowModel> getAllModels(@NotNull final Module module) {
    return myModelFactory.getAllModels(module);
  }
}
