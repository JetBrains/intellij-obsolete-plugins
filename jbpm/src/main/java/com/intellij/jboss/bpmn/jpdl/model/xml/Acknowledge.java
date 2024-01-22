package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jbpm.org/4.3/jpdm:acknowledgeType enumeration.
 */
public enum Acknowledge implements NamedEnum {
  AUTO("auto"),
  CLIENT("client"),
  DUPS_OK("dups-ok");

  private final String value;

  Acknowledge(String value) { this.value = value; }

  @Override
  public String getValue() { return value; }
}
