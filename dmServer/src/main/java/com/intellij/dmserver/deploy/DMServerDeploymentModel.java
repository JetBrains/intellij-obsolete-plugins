package com.intellij.dmserver.deploy;

import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.appServers.deployment.DeploymentSource;
import com.intellij.javaee.appServers.run.configuration.CommonModel;

public class DMServerDeploymentModel extends DeploymentModel {
  public DMServerDeploymentModel(CommonModel parentConfiguration, DeploymentSource source) {
    super(parentConfiguration, source);
  }


}
