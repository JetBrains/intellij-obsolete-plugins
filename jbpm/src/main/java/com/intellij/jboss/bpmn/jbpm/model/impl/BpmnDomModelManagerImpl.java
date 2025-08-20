package com.intellij.jboss.bpmn.jbpm.model.impl;

import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModel;
import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModelManager;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TDefinitions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BpmnDomModelManagerImpl extends BpmnDomModelManager {
  private final BpmnDomModelFactory myDomModelFactory;

  public BpmnDomModelManagerImpl(Project project) {
    myDomModelFactory = new BpmnDomModelFactory(project);
  }

  @Override
  public boolean isBpmnDomModel(@NotNull final XmlFile file) {
    return DomManager.getDomManager(file.getProject()).getFileElement(file, TDefinitions.class) != null;
  }

  @Override
  @Nullable
  public BpmnDomModel getModel(@NotNull final XmlFile file) {
    return myDomModelFactory.getModelByConfigFile(file);
  }

  @Override
  public List<BpmnDomModel> getAllModels(@NotNull final Module module) {
    return myDomModelFactory.getAllModels(module);
  }
}
