package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class ConnectorDeployCommand extends ConnectorFileDeployCommandBase {

  private final DeploymentModel myDeploymentModel;

  public ConnectorDeployCommand(DMServerInstance dmServer, @NotNull DeploymentModel deploymentModel, @NotNull VirtualFile fileToDeploy) {
    super(dmServer, fileToDeploy);
    myDeploymentModel = deploymentModel;
  }

  @Override
  protected String getPath(VirtualFile fileToDeploy) {
    return myDeploymentModel.getDeploymentSource().getFilePath();
  }
}
