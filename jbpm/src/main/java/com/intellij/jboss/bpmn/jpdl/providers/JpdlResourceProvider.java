package com.intellij.jboss.bpmn.jpdl.providers;

import com.intellij.javaee.ResourceRegistrar;
import com.intellij.javaee.StandardResourceProvider;

public class JpdlResourceProvider implements StandardResourceProvider {

  @Override
  public void registerResources(ResourceRegistrar registrar) {
    registrar.addStdResource("http://jbpm.org/4.3/jpdl", "/schemas/jpdl-4.3.xsd", getClass());
    registrar.addStdResource("http://jbpm.org/4.2/jpdl", "/schemas/jpdl-4.2.xsd", getClass());
    registrar.addStdResource("http://jbpm.org/4.0/jpdl", "/schemas/jpdl-4.0.xsd", getClass());
  }
}