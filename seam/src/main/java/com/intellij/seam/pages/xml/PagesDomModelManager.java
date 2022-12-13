package com.intellij.seam.pages.xml;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class PagesDomModelManager {
  public static PagesDomModelManager getInstance(Project project) {
    return project.getService(PagesDomModelManager.class);
  }

  public abstract boolean isPages(@NotNull final XmlFile file) ;

  @Nullable
  public abstract PagesModel getPagesModel(@NotNull final XmlFile file);

   public abstract List<PagesModel> getAllModels(@NotNull Module module);
}

