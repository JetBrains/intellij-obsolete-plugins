package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jbpm.org/4.3/jpdm:migrationActionType enumeration.
 */
public enum MigrationAction implements NamedEnum {
  END("end"),
  MIGRATE("migrate");

  private final String value;

  MigrationAction(String value) { this.value = value; }

  @Override
  public String getValue() { return value; }
}
