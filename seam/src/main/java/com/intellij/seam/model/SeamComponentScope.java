package com.intellij.seam.model;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/components:scopeAttrType enumeration.
 */
public enum SeamComponentScope implements NamedEnum {
  UNSPECIFIED("UNSPECIFIED"),
  APPLICATION("APPLICATION"),
  BUSINESS_PROCESS("BUSINESS_PROCESS"),
  CONVERSATION("CONVERSATION"),
  EVENT("EVENT"),
  PAGE("PAGE"),
  SESSION("SESSION"),
  STATELESS("STATELESS"),
  METHOD("METHOD"),
  APPLICATION_LOWERCASE("application"),
  BUSINESS_PROCESS_LOWERCASE("business_process"),
  CONVERSATION_LOWERCASE("conversation"),
  EVENT_LOWERCASE("event"),
  PAGE_LOWERCASE("page"),
  SESSION_LOWERCASE("session"),
  STATELESS_LOWERCASE("stateless");

  private final String value;

  SeamComponentScope(String value) {
    this.value = value;
  }

  @Override
  @NlsSafe
  public String getValue() {
    return value;
  }

  public boolean isEqual(SeamComponentScope scope) {
    return StringUtil.toLowerCase(scope.getValue()).equals(StringUtil.toLowerCase(getValue()));
  }
}
