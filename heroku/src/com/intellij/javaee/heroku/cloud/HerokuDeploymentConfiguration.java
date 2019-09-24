package com.intellij.javaee.heroku.cloud;

import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.util.CloudDeploymentNameConfiguration;
import com.intellij.remoteServer.util.CloudDeploymentNameProvider;
import com.intellij.remoteServer.util.CloudGitDeploymentChecker;
import org.jetbrains.annotations.NotNull;

/**
 * @author michael.golubev
 */
public class HerokuDeploymentConfiguration extends CloudDeploymentNameConfiguration<HerokuDeploymentConfiguration> {

  public static final CloudDeploymentNameProvider DEPLOYMENT_NAME_PROVIDER = new CloudDeploymentNameProvider() {

    @NotNull
    @Override
    public String getDeploymentName(@NotNull DeploymentSource deploymentSource) {
      return CloudDeploymentNameProvider.DEFAULT_NAME_PROVIDER.getDeploymentName(deploymentSource).replace('_', '-');
    }
  };

  private static final CloudGitDeploymentChecker<HerokuDeploymentConfiguration, HerokuCloudConfiguration, HerokuServerRuntimeInstance>
    ourCloudGitDeploymentChecker
    = new CloudGitDeploymentChecker<>(
    new HerokuDeploymentDetector());

  private Integer myDebugPort;
  private String myHost;

  public Integer getDebugPort() {
    return myDebugPort;
  }

  public void setDebugPort(Integer debugPort) {
    myDebugPort = debugPort;
  }

  public String getHost() {
    return myHost;
  }

  public void setHost(String host) {
    myHost = host;
  }

  @Override
  protected String getDefaultDeploymentSourceName(DeploymentSource deploymentSource) {
    return DEPLOYMENT_NAME_PROVIDER.getDeploymentName(deploymentSource);
  }

  @Override
  public void checkConfiguration(RemoteServer<?> server, DeploymentSource deploymentSource) throws RuntimeConfigurationException {
    ourCloudGitDeploymentChecker.checkGitUrl((RemoteServer<HerokuCloudConfiguration>)server, deploymentSource, this);
  }
}
