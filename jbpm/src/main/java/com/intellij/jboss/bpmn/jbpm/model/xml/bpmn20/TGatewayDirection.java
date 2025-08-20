package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.NamedEnum;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tGatewayDirection enumeration.
 */
public enum TGatewayDirection implements NamedEnum {
  CONVERGING("Converging"),
  DIVERGING("Diverging"),
  MIXED("Mixed"),
  UNSPECIFIED("Unspecified");

  private final String value;

  TGatewayDirection(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }
}
