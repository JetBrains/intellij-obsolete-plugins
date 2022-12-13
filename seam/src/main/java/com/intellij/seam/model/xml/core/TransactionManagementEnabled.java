package com.intellij.seam.model.xml.core;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/core:transaction-management-enabledAttrType enumeration.
 */
public enum TransactionManagementEnabled implements NamedEnum {
  FALSE("false"),
  TRUE("true");

  private final String value;

  TransactionManagementEnabled(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

}
