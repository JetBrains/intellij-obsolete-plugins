package com.intellij.dmserver.install.impl;

import com.intellij.dmserver.deploy.DeploymentIdentity;

public class ServerVersionVirgo36 extends ServerVersionVirgo35 {

  @Override
  protected DeploymentIdentity createAdminIdentity() {
    return new DeploymentIdentity("org.eclipse.virgo.management", "*", "plan", "global");
  }
}
