package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jbpm.org/4.3/jpdm:lockmodeAttrType enumeration.
 */
public enum Lockmode implements NamedEnum {
  NONE("none"),
  READ("read"),
  UPGRADE("upgrade"),
  UPGRADE_NOWAIT("upgrade_nowait"),
  WRITE("write");

  private final String value;

  Lockmode(String value) { this.value = value; }

  @Override
  public String getValue() { return value; }
}
