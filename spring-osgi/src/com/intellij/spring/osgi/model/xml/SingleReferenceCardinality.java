// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml;

import com.intellij.util.xml.NamedEnum;

public enum SingleReferenceCardinality implements NamedEnum {
  TsingleReferenceCardinality_0__1("0..1"),
  TsingleReferenceCardinality_1__1("1..1");

  private final String value;

  SingleReferenceCardinality(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

}
