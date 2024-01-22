package com.intellij.jboss.bpmn.jpdl.impl;

import com.intellij.jboss.bpmn.jpdl.model.JpdlModel;
import com.intellij.jboss.bpmn.jpdl.model.xml.ProcessDefinition;
import com.intellij.openapi.module.Module;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.model.impl.DomModelImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class JpdlModelImpl extends DomModelImpl<ProcessDefinition> implements JpdlModel {
  private final Module myModule;


  public JpdlModelImpl(@NotNull Module module, @NotNull DomFileElement<ProcessDefinition> mergedModel, @NotNull Set<XmlFile> configFiles) {
    super(mergedModel, configFiles);

    myModule = module;
  }

  public Module getModule() {
    return myModule;
  }
}
