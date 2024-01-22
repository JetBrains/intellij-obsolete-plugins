package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.NamedEnum;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tItemKind enumeration.
 */
public enum TItemKind implements NamedEnum {
  INFORMATION("Information"),
  PHYSICAL("Physical");

  private final String value;

  TItemKind(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }
}
