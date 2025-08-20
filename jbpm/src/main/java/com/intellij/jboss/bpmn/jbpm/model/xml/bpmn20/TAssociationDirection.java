package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.NamedEnum;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tAssociationDirection enumeration.
 */
public enum TAssociationDirection implements NamedEnum {
  BOTH("Both"),
  NONE("None"),
  ONE("One");

  private final String value;

  TAssociationDirection(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }
}
