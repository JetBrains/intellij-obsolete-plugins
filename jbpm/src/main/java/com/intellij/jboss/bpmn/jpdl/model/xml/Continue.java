package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jbpm.org/4.3/jpdm:continueType enumeration.
 */
public enum Continue implements NamedEnum {
  ASYNC("async"),
  EXCLUSIVE("exclusive"),
  SYNC("sync");

  private final String value;

  Continue(String value) { this.value = value; }

  @Override
  public String getValue() { return value; }
}
