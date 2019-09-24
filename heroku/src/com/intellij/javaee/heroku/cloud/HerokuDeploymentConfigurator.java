package com.intellij.javaee.heroku.cloud;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.util.CloudDeploymentConfiguratorBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author michael.golubev
 */
public class HerokuDeploymentConfigurator
  extends CloudDeploymentConfiguratorBase<HerokuDeploymentConfiguration, HerokuCloudConfiguration> {

  public HerokuDeploymentConfigurator(Project project) {
    super(project, HerokuCloudType.getInstance());
  }

  @NotNull
  @Override
  public HerokuDeploymentConfiguration createDefaultConfiguration(@NotNull DeploymentSource source) {
    return new HerokuDeploymentConfiguration();
  }

  @Nullable
  @Override
  public SettingsEditor<HerokuDeploymentConfiguration> createEditor(@NotNull DeploymentSource source, @NotNull RemoteServer<HerokuCloudConfiguration> server) {
    return new HerokuDeploymentEditor();
  }
}
