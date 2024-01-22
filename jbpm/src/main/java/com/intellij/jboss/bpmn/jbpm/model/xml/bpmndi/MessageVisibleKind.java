// Generated on Tue Jun 12 14:10:06 CEST 2012
// DTD/Schema  :    http://www.omg.org/spec/BPMN/20100524/DI

package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi;

import com.intellij.jboss.bpmn.jbpm.constants.JbpmNamespaceConstants;
import com.intellij.util.xml.NamedEnum;
import com.intellij.util.xml.Namespace;

/**
 * http://www.omg.org/spec/BPMN/20100524/DI:MessageVisibleKind enumeration.
 */
@Namespace(JbpmNamespaceConstants.JBPM_20_DI_NAMESPACE_KEY)
public enum MessageVisibleKind implements NamedEnum {
  INITIATING("initiating"),
  NON_INITIATING("non_initiating");

  private final String value;

  MessageVisibleKind(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }
}
