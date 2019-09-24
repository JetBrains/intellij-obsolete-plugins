package com.intellij.javaee.heroku.cloud.module;

import com.intellij.javaee.heroku.cloud.HerokuCloudConfiguration;
import com.intellij.javaee.heroku.cloud.HerokuDeploymentConfiguration;
import com.intellij.javaee.heroku.cloud.HerokuModuleDeploymentRuntime;
import com.intellij.javaee.heroku.cloud.HerokuServerRuntimeInstance;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfiguration;
import com.intellij.remoteServer.impl.module.CloudModuleBuilder;
import com.intellij.remoteServer.impl.module.CloudModuleBuilderSourceContribution;
import com.intellij.remoteServer.util.ServerRuntimeException;

/**
 * @author michael.golubev
 */
public class HerokuModuleBuilderContribution extends CloudModuleBuilderSourceContribution<
  HerokuCloudConfiguration,
  HerokuDeploymentConfiguration,
  HerokuApplicationConfiguration,
  HerokuServerRuntimeInstance> {

  public HerokuModuleBuilderContribution(CloudModuleBuilder moduleBuilder, ServerType<HerokuCloudConfiguration> cloudType) {
    super(moduleBuilder, cloudType);
  }

  @Override
  protected HerokuDeploymentConfiguration createDeploymentConfiguration() {
    return new HerokuDeploymentConfiguration();
  }

  @Override
  protected HerokuApplicationConfigurable createApplicationConfigurable(Project project, Disposable parentDisposable) {
    return new HerokuApplicationConfigurable(project, parentDisposable);
  }

  @Override
  protected void doConfigureModule(HerokuApplicationConfiguration applicationConfiguration,
                                   DeployToServerRunConfiguration<HerokuCloudConfiguration, HerokuDeploymentConfiguration> runConfiguration,
                                   boolean firstAttempt,
                                   HerokuServerRuntimeInstance serverRuntime) throws ServerRuntimeException {
    HerokuModuleDeploymentRuntime deploymentRuntime
      = (HerokuModuleDeploymentRuntime)serverRuntime.createDeploymentRuntime(runConfiguration);
    if (applicationConfiguration.isTemplate()) {
      if (firstAttempt) {
        deploymentRuntime.createApplicationByTemplate(applicationConfiguration.getTemplate().getGitUrl());
      }
      else {
        deploymentRuntime.fetchAndRefresh();
      }
    }
    else if (applicationConfiguration.isExisting()) {
      deploymentRuntime.downloadExistingApplication();
    }
  }
}
