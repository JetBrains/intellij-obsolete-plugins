package com.intellij.jboss.bpmn.jbpm.providers;

import com.intellij.javaee.ResourceRegistrar;
import com.intellij.javaee.StandardResourceProvider;

public class JbpmResourceProvider implements StandardResourceProvider {

  @Override
  public void registerResources(ResourceRegistrar registrar) {
    registrar.addStdResource("http://www.omg.org/spec/BPMN/20100524/MODEL", "/schemas/jbmn20/BPMN20.xsd", getClass());
    registrar.addStdResource("BPMN20.xsd", "/schemas/jbmn20/BPMN20.xsd", getClass());
    registrar.addStdResource("http://www.omg.org/spec/BPMN/20100524/DI", "/schemas/jbmn20/BPMNDI.xsd", getClass());
    registrar.addStdResource("http://www.omg.com/dd/1.0.0", "/schemas/jbmn20/DiagramDefinition.xsd", getClass());
    registrar.addStdResource("http://www.omg.com/di/1.0.0", "/schemas/jbmn20/DiagramInterchange.xsd", getClass());
    registrar.addStdResource("http://www.omg.org/spec/DD/20100524/DC", "/schemas/jbmn20/DC.xsd", getClass());
    registrar.addStdResource("http://www.omg.org/spec/DD/20100524/DI", "/schemas/jbmn20/DI.xsd", getClass());
  }
}