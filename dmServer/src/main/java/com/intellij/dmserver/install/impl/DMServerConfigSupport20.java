package com.intellij.dmserver.install.impl;

import com.intellij.openapi.vfs.VirtualFile;

public class DMServerConfigSupport20 extends DMServerConfigSupport2Base {

  public DMServerConfigSupport20(VirtualFile home) {
    super("config/com.springsource.kernel.properties",
          "config/com.springsource.osgi.medic.properties",
          "config/com.springsource.repository.properties",
          home);
  }
}
