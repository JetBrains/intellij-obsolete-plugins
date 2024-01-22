// Generated on Tue Jun 12 14:10:06 CEST 2012
// DTD/Schema  :    http://www.omg.org/spec/BPMN/20100524/DI

package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi;

import com.intellij.jboss.bpmn.jbpm.constants.JbpmNamespaceConstants;
import com.intellij.util.xml.NamedEnum;
import com.intellij.util.xml.Namespace;

/**
 * http://www.omg.org/spec/BPMN/20100524/DI:ParticipantBandKind enumeration.
 */
@Namespace(JbpmNamespaceConstants.JBPM_20_DI_NAMESPACE_KEY)
public enum ParticipantBandKind implements NamedEnum {
  BOTTOM_INITIATING("bottom_initiating"),
  BOTTOM_NON_INITIATING("bottom_non_initiating"),
  MIDDLE_INITIATING("middle_initiating"),
  MIDDLE_NON_INITIATING("middle_non_initiating"),
  TOP_INITIATING("top_initiating"),
  TOP_NON_INITIATING("top_non_initiating");

  private final String value;

  ParticipantBandKind(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }
}
