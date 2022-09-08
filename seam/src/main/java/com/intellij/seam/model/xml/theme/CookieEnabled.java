package com.intellij.seam.model.xml.theme;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/theme:cookie-enabledAttrType enumeration.
 */
public enum CookieEnabled implements NamedEnum {
  FALSE("false"),
  TRUE("true");

  private final String value;

  CookieEnabled(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

}
