package com.intellij.jboss.bpmn.jbpm.model;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public abstract class BpmnDomModelManager {

  public abstract boolean isBpmnDomModel(@NotNull final XmlFile file);

  @Nullable
  public abstract BpmnDomModel getModel(@NotNull final XmlFile file);

  public abstract List<BpmnDomModel> getAllModels(@NotNull Module module);

  public static BpmnDomModelManager getInstance(Project project) {
    return project.getService(BpmnDomModelManager.class);
  }
}