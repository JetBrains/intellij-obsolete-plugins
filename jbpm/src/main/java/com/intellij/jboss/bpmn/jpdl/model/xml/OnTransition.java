package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jbpm.org/4.3/jpdm:on-transitionAttrType enumeration.
 */
public enum OnTransition implements NamedEnum {
  CANCEL("cancel"),
  KEEP("keep");

  private final String value;

  OnTransition(String value) { this.value = value; }

  @Override
  public String getValue() { return value; }
}
