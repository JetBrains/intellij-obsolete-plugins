package com.intellij.jboss.bpmn.jpdl.model;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class JpdlDomModelManager {
  public abstract boolean isJpdl(@NotNull final XmlFile file);

  public abstract JpdlModel getJpdlModel(@NotNull final XmlFile file);

  public abstract List<JpdlModel> getAllModels(@NotNull Module module);

  public static JpdlDomModelManager getInstance(Project project) {
    return project.getService(JpdlDomModelManager.class);
  }
}
