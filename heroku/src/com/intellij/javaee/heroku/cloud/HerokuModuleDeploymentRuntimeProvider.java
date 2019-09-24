package com.intellij.javaee.heroku.cloud;

import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;
import com.intellij.remoteServer.runtime.deployment.DeploymentLogManager;
import com.intellij.remoteServer.runtime.deployment.DeploymentTask;
import com.intellij.remoteServer.util.*;

import java.io.File;

/**
 * @author michael.golubev
 */
public class HerokuModuleDeploymentRuntimeProvider extends CloudModuleDeploymentRuntimeProviderBase {

  @Override
  public ServerType<?> getServerType() {
    return HerokuCloudType.getInstance();
  }

  @Override
  protected CloudDeploymentRuntime doCreateDeploymentRuntime(ModuleDeploymentSource moduleSource,
                                                             File contentRootFile,
                                                             CloudMultiSourceServerRuntimeInstance serverRuntime,
                                                             DeploymentTask<? extends CloudDeploymentNameConfiguration> deploymentTask,
                                                             DeploymentLogManager logManager) throws ServerRuntimeException {
    return new HerokuModuleDeploymentRuntime(serverRuntime, moduleSource, contentRootFile, deploymentTask, logManager);
  }
}
