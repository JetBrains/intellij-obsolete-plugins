package com.intellij.dmserver.install.impl;

import com.intellij.dmserver.deploy.DeploymentIdentity;
import com.intellij.dmserver.install.DMServerConfigSupport;
import com.intellij.openapi.vfs.VirtualFile;

public class ServerVersion21Virgo extends ServerVersion2Base {

  public ServerVersion21Virgo() {
    super("org.eclipse.virgo");
  }

  @Override
  public DMServerConfigSupport createConfigSupport(VirtualFile home) {
    return new DMServerConfigurationSupportVirgo(home, "config");
  }

  @Override
  protected DeploymentIdentity createAdminIdentity() {
    return new DeploymentIdentity("org.eclipse.virgo.apps.admin.plan", "*", "plan");
  }

  @Override
  protected DeploymentIdentity createRepositoryIdentity() {
    return new DeploymentIdentity("org.eclipse.virgo.apps.repository", "*", "par");
  }
}
