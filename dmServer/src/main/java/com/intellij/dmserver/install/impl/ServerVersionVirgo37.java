package com.intellij.dmserver.install.impl;

import com.intellij.dmserver.deploy.DeploymentIdentity;
import org.jetbrains.annotations.Nullable;

public class ServerVersionVirgo37 extends ServerVersionVirgo36 {

  @Override
  @Nullable
  protected DeploymentIdentity createRepositoryIdentity() {
    return null;
  }
}
