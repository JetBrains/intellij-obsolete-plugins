// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml;

import com.intellij.util.xml.NamedEnum;

public enum CollectionCardinality implements NamedEnum {
  CollectionCardinality_0__N("0..N"),
  CollectionCardinality_1__N("1..N");

  private final String value;

  CollectionCardinality(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

}
