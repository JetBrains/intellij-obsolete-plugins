package com.intellij.jboss.bpmn.jbpm.providers;

import com.intellij.jboss.bpmn.jbpm.constants.JbpmNamespaceConstants;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TDefinitions;
import com.intellij.util.xml.DomFileDescription;

public class JbpmDomFileDescription extends DomFileDescription<TDefinitions> {

  public JbpmDomFileDescription() {
    super(TDefinitions.class, "definitions");
  }

  @Override
  protected void initializeFileDescription() {
    registerNamespacePolicy(JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY, JbpmNamespaceConstants.JBPM_20_NAMESPACE,
                            JbpmNamespaceConstants.JBPM_20_SHORT_NAMESPACE);
    registerNamespacePolicy(JbpmNamespaceConstants.JBPM_20_DI_NAMESPACE_KEY, JbpmNamespaceConstants.JBPM_20_DI_NAMESPACE);

    registerNamespacePolicy(JbpmNamespaceConstants.OMG_DC_NAMESPACE_KEY, JbpmNamespaceConstants.OMG_DC_NAMESPACE);
    registerNamespacePolicy(JbpmNamespaceConstants.OMG_DI_NAMESPACE_KEY, JbpmNamespaceConstants.OMG_DI_NAMESPACE);
  }
}
