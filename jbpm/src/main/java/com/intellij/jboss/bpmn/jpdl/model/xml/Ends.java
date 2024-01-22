package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jbpm.org/4.3/jpdm:endsAttrType enumeration.
 */
public enum Ends implements NamedEnum {
  EXECUTION("execution"),
  PROCESS_INSTANCE("process-instance");

  private final String value;

  Ends(String value) { this.value = value; }

  @Override
  public String getValue() { return value; }
}
