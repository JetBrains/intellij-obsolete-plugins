package com.intellij.dmserver.install.impl;

import com.intellij.dmserver.deploy.DeploymentIdentity;
import org.jetbrains.annotations.Nullable;

public class ServerVersionVirgo3 extends ServerVersion21Virgo {

  @Override
  protected String getDeploymentMBeanType() {
    return "ArtifactModel";
  }

  @Override
  protected String getDeploymentMBeanRegion() {
    return "org.eclipse.virgo.region.user";
  }

  @Override
  protected DeploymentIdentity createAdminIdentity() {
    return new DeploymentIdentity("org.eclipse.virgo.apps.admin.plan", "*", "plan", "global");
  }

  @Nullable
  @Override
  protected DeploymentIdentity createNeedForRepositoryIdentity() {
    return new DeploymentIdentity("org.eclipse.virgo.web.tomcat", "*", "plan", "global");
  }

  @Override
  protected DeploymentIdentity createRepositoryIdentity() {
    return new DeploymentIdentity("org.eclipse.virgo.apps.repository", "*", "par", "global");
  }
}
