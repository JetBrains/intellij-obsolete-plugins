package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.NamedEnum;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tChoreographyLoopType enumeration.
 */
public enum TChoreographyLoop implements NamedEnum {
  MULTI_INSTANCE_PARALLEL("MultiInstanceParallel"),
  MULTI_INSTANCE_SEQUENTIAL("MultiInstanceSequential"),
  NONE("None"),
  STANDARD("Standard");

  private final String value;

  TChoreographyLoop(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }
}
