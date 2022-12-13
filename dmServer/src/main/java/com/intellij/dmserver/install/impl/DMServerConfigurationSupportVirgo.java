package com.intellij.dmserver.install.impl;

import com.intellij.openapi.vfs.VirtualFile;

public class DMServerConfigurationSupportVirgo extends DMServerConfigSupport2Base {

  public DMServerConfigurationSupportVirgo(VirtualFile home, String configDir) {
    super(configDir + "/org.eclipse.virgo.kernel.properties",
          configDir + "/org.eclipse.virgo.medic.properties",
          configDir + "/org.eclipse.virgo.repository.properties",
          home);
  }
}
