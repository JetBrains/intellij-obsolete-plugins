package com.intellij.jboss.bpmn.jbpm.model;

import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TDefinitions;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.model.DomModel;
import org.jetbrains.annotations.NotNull;

public interface BpmnDomModel extends DomModel<TDefinitions> {

  @NotNull
  TDefinitions getDefinitions();

  @NotNull
  XmlFile getFlowFile();
}