package com.intellij.seam.pageflow.model.xml.pageflow;

import com.intellij.util.xml.NamedEnum;

public enum Enabled implements NamedEnum {
  DISABLED("disabled"),
  ENABLED("enabled");

  private final String value;

  Enabled(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

}
