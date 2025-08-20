package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.NamedEnum;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tEventBasedGatewayType enumeration.
 */
public enum TEventBasedGatewayType implements NamedEnum {
  EXCLUSIVE("Exclusive"),
  PARALLEL("Parallel");

  private final String value;

  TEventBasedGatewayType(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }
}
