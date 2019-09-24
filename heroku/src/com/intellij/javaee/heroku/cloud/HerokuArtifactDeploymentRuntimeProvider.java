package com.intellij.javaee.heroku.cloud;

import com.intellij.javaee.ui.packaging.WarArtifactType;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.deployment.ArtifactDeploymentSource;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.configuration.deployment.JavaDeploymentSourceUtil;
import com.intellij.remoteServer.impl.util.ArtifactDeploymentRuntimeProviderBase;
import com.intellij.remoteServer.runtime.deployment.DeploymentLogManager;
import com.intellij.remoteServer.runtime.deployment.DeploymentTask;
import com.intellij.remoteServer.util.CloudDeploymentNameConfiguration;
import com.intellij.remoteServer.util.CloudDeploymentRuntime;
import com.intellij.remoteServer.util.CloudMultiSourceServerRuntimeInstance;
import com.intellij.remoteServer.util.ServerRuntimeException;

import java.io.File;
import java.util.Collection;

/**
 * @author michael.golubev
 */
public class HerokuArtifactDeploymentRuntimeProvider extends ArtifactDeploymentRuntimeProviderBase {

  @Override
  public ServerType<?> getServerType() {
    return HerokuCloudType.getInstance();
  }

  @Override
  public Collection<DeploymentSource> getDeploymentSources(Project project) {
    return JavaDeploymentSourceUtil.getInstance().createArtifactDeploymentSources(project, WarArtifactType.getInstance());
  }

  @Override
  protected CloudDeploymentRuntime doCreateDeploymentRuntime(ArtifactDeploymentSource artifactSource,
                                                             File artifactFile,
                                                             CloudMultiSourceServerRuntimeInstance serverRuntime,
                                                             DeploymentTask<? extends CloudDeploymentNameConfiguration> deploymentTask,
                                                             DeploymentLogManager logManager) throws ServerRuntimeException {
    return new HerokuArtifactDeploymentRuntime(serverRuntime, artifactSource, artifactFile, deploymentTask, logManager);
  }
}
