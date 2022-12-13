package com.intellij.dmserver.install.impl;

import com.intellij.dmserver.integration.DMServerIntegrationData;

public interface ConfigElementWrapper<T> {

  T getValue(DMServerIntegrationData data);

  void setValue(DMServerIntegrationData data, T value);
}
