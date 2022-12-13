package com.intellij.seam.pageflow.model.xml;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class PageflowDomModelManager {
  public static PageflowDomModelManager getInstance(Project project) {
    return project.getService(PageflowDomModelManager.class);
  }

  public abstract boolean isPageflow(@NotNull final XmlFile file) ;

  public abstract PageflowModel getPageflowModel(@NotNull final XmlFile file);

   public abstract List<PageflowModel> getAllModels(@NotNull Module module);
}
