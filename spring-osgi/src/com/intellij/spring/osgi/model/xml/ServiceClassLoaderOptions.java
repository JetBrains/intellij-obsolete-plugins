// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml;

import com.intellij.util.xml.NamedEnum;

public enum ServiceClassLoaderOptions implements NamedEnum {
  SERVICE_PROVIDER("service-provider"),
  UNMANAGED("unmanaged");

  private final String value;

  ServiceClassLoaderOptions(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

}
