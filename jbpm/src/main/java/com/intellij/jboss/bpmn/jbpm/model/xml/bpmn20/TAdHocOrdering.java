package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.NamedEnum;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tAdHocOrdering enumeration.
 */
public enum TAdHocOrdering implements NamedEnum {
  PARALLEL("Parallel"),
  SEQUENTIAL("Sequential");

  private final String value;

  TAdHocOrdering(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }
}
