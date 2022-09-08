package com.intellij.seam.model.xml.core;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/core:debugAttrType enumeration.
 */
public enum Debug implements NamedEnum {
  FALSE("false"),
  TRUE("true");

  private final String value;

  Debug(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

}
