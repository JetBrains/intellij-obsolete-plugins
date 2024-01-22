package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.NamedEnum;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tMultiInstanceFlowCondition enumeration.
 */
public enum TMultiInstanceFlowCondition implements NamedEnum {
  ALL("All"),
  COMPLEX("Complex"),
  NONE("None"),
  ONE("One");

  private final String value;

  TMultiInstanceFlowCondition(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }
}
