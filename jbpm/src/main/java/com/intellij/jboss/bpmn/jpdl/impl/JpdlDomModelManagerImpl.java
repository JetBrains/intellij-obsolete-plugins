package com.intellij.jboss.bpmn.jpdl.impl;

import com.intellij.jboss.bpmn.jpdl.model.JpdlDomModelManager;
import com.intellij.jboss.bpmn.jpdl.model.JpdlModel;
import com.intellij.jboss.bpmn.jpdl.model.JpdlModelFactory;
import com.intellij.jboss.bpmn.jpdl.model.xml.ProcessDefinition;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class JpdlDomModelManagerImpl extends JpdlDomModelManager {
  private final JpdlModelFactory myModelFactory;
  private final DomManager myDomManager;

  JpdlDomModelManagerImpl(@NotNull Project project) {
    myDomManager = DomManager.getDomManager(project);
    myModelFactory = new JpdlModelFactory(project);
  }

  @Override
  public boolean isJpdl(@NotNull final XmlFile file) {
    return myDomManager.getFileElement(file, ProcessDefinition.class) != null;
  }

  @Override
  public JpdlModel getJpdlModel(@NotNull final XmlFile file) {
    return myModelFactory.getModelByConfigFile(file);
  }

  @Override
  public List<JpdlModel> getAllModels(@NotNull final Module module) {
    return myModelFactory.getAllModels(module);
  }
}
