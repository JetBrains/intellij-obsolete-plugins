package com.intellij.dmserver.install;

import com.intellij.dmserver.integration.DMServerIntegrationData;

public interface DMServerConfigSupport {
  void readFromServer(DMServerIntegrationData data);

  void writeToServer(DMServerIntegrationData data);

  boolean isValid();
}
