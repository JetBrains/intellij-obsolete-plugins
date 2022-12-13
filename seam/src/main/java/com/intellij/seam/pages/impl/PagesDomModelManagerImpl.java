package com.intellij.seam.pages.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.pages.xml.PagesDomModelManager;
import com.intellij.seam.pages.xml.PagesModel;
import com.intellij.seam.pages.xml.PagesModelFactory;
import com.intellij.seam.pages.xml.pages.Pages;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PagesDomModelManagerImpl extends PagesDomModelManager {
 private final PagesModelFactory myModelFactory;
 private final DomManager myDomManager;

  public PagesDomModelManagerImpl(final Project project) {
    myModelFactory = new PagesModelFactory(project);
    myDomManager = DomManager.getDomManager(project);
  }

  @Override
  public boolean isPages(@NotNull final XmlFile file) {
    return myDomManager.getFileElement(file, Pages.class) != null;
  }

  @Override
  public PagesModel getPagesModel(@NotNull final XmlFile file) {
    return myModelFactory.getModelByConfigFile(file);
  }

  @Override
  public List<PagesModel> getAllModels(@NotNull final Module module) {
    return myModelFactory.getAllModels(module);
  }
}

