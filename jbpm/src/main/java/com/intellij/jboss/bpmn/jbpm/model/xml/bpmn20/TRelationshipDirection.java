package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.NamedEnum;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tRelationshipDirection enumeration.
 */
public enum TRelationshipDirection implements NamedEnum {
  BACKWARD("Backward"),
  BOTH("Both"),
  FORWARD("Forward"),
  NONE("None");

  private final String value;

  TRelationshipDirection(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }
}
