// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml;

import com.intellij.util.xml.NamedEnum;

public enum DefaultCardinalityOptions implements NamedEnum {
  TdefaultCardinalityOptions_0__X("0..X"),
  TdefaultCardinalityOptions_1__X("1..X");

  private final String value;

  DefaultCardinalityOptions(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

}
