package com.intellij.dmserver.install.impl;

import com.intellij.dmserver.install.DMServerConfigSupport;
import com.intellij.openapi.vfs.VirtualFile;

public class ServerVersionVirgo35 extends ServerVersionVirgo3 {

  @Override
  public DMServerConfigSupport createConfigSupport(VirtualFile home) {
    return new DMServerConfigurationSupportVirgo(home, "configuration");
  }
}
