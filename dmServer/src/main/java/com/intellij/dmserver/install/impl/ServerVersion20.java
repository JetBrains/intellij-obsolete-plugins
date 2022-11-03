package com.intellij.dmserver.install.impl;

import com.intellij.dmserver.deploy.DeploymentIdentity;
import com.intellij.dmserver.install.DMServerConfigSupport;
import com.intellij.openapi.vfs.VirtualFile;

public class ServerVersion20 extends ServerVersion2Base {

  public ServerVersion20() {
    super("com.springsource");
  }

  @Override
  public DMServerConfigSupport createConfigSupport(VirtualFile home) {
    return new DMServerConfigSupport20(home);
  }

  @Override
  protected DeploymentIdentity createAdminIdentity() {
    return new DeploymentIdentity("com.springsource.server.admin.plan", "*", "plan");
  }

  @Override
  protected DeploymentIdentity createRepositoryIdentity() {
    return new DeploymentIdentity("com.springsource.server.repository.hosted", "*", "par");
  }
}
