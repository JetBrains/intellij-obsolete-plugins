package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jbpm.org/4.3/jpdm:booleanValueType enumeration.
 */
public enum BooleanValue implements NamedEnum {
  DISABLED("disabled"),
  ENABLED("enabled"),
  FALSE("false"),
  OFF("off"),
  ON("on"),
  TRUE("true");

  private final String value;

  BooleanValue(String value) { this.value = value; }

  @Override
  public String getValue() { return value; }
}
