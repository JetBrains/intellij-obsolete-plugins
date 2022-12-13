package com.intellij.seam.model.xml;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.model.xml.components.SeamComponents;
import com.intellij.util.xml.converters.values.GenericDomValueConvertersRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class SeamDomModelManager {
  public static SeamDomModelManager getInstance(Project project) {
    return project.getService(SeamDomModelManager.class);
  }

  public abstract boolean isSeamComponents(@NotNull XmlFile file);

  public abstract @Nullable SeamComponents getSeamModel(@NotNull XmlFile file);

  public abstract @NotNull List<SeamComponents> getAllModels(@NotNull Module module);

  public abstract GenericDomValueConvertersRegistry getValueConvertersRegistry();
}
