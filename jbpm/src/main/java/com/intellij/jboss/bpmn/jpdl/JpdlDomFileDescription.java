package com.intellij.jboss.bpmn.jpdl;

import com.intellij.jboss.bpmn.jpdl.constants.JpdlNamespaceConstants;
import com.intellij.jboss.bpmn.jpdl.model.xml.ProcessDefinition;
import com.intellij.util.xml.DomFileDescription;

public class JpdlDomFileDescription extends DomFileDescription<ProcessDefinition> {

  public JpdlDomFileDescription() {
    super(ProcessDefinition.class, "process");
  }

  @Override
  protected void initializeFileDescription() {
    registerNamespacePolicy(JpdlNamespaceConstants.JPDL_NAMESPACE_KEY,
                            JpdlNamespaceConstants.JPDL_NAMESPACE_4_4, JpdlNamespaceConstants.JPDL_NAMESPACE_4_3,
                            JpdlNamespaceConstants.JPDL_NAMESPACE_4_2, JpdlNamespaceConstants.JPDL_NAMESPACE_4_0);
  }
}
