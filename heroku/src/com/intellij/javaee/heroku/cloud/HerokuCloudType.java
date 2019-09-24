package com.intellij.javaee.heroku.cloud;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.RemoteServerConfigurable;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator;
import com.intellij.remoteServer.runtime.ServerConnector;
import com.intellij.remoteServer.runtime.ServerTaskExecutor;
import com.intellij.remoteServer.runtime.deployment.debug.*;
import com.intellij.remoteServer.util.CloudDeploymentRuntime;
import com.intellij.remoteServer.util.ServerRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author michael.golubev
 */
public class HerokuCloudType extends ServerType<HerokuCloudConfiguration> {
  public static final String HELP_TOPIC = "reference.heroku";

  public static HerokuCloudType getInstance() {
    return EP_NAME.findExtension(HerokuCloudType.class);
  }

  public HerokuCloudType() {
    super("heroku");
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Heroku";
  }

  @NotNull
  @Override
  public String getHelpTopic() {
    return HELP_TOPIC;
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.General.Balloon;
  }

  @NotNull
  @Override
  public HerokuCloudConfiguration createDefaultConfiguration() {
    return new HerokuCloudConfiguration();
  }

  @NotNull
  @Override
  public RemoteServerConfigurable createServerConfigurable(@NotNull HerokuCloudConfiguration configuration) {
    return new HerokuCloudConfigurable(configuration);
  }

  @NotNull
  @Override
  public DeploymentConfigurator<?, HerokuCloudConfiguration> createDeploymentConfigurator(Project project) {
    return new HerokuDeploymentConfigurator(project);
  }

  @NotNull
  @Override
  public ServerConnector<?> createConnector(@NotNull RemoteServer<HerokuCloudConfiguration> server,
                                            @NotNull ServerTaskExecutor asyncTasksExecutor) {
    return new HerokuConnector(server, asyncTasksExecutor);
  }

  @NotNull
  @Override
  public ServerConnector<?> createConnector(@NotNull HerokuCloudConfiguration configuration,
                                            @NotNull ServerTaskExecutor asyncTasksExecutor) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public DebugConnector<?, ?> createDebugConnector() {
    return new DebugConnector<JavaDebugConnectionData, CloudDeploymentRuntime>() {

      @NotNull
      @Override
      public DebuggerLauncher<JavaDebugConnectionData> getLauncher() {
        return JavaDebuggerLauncher.getInstance();
      }

      @NotNull
      @Override
      public JavaDebugConnectionData getConnectionData(@NotNull final CloudDeploymentRuntime runtime)
        throws DebugConnectionDataNotAvailableException {
        try {
          return ((HerokuDebugConnectionProvider)runtime).getDebugConnectionData();
        }
        catch (ServerRuntimeException e) {
          throw new DebugConnectionDataNotAvailableException(e.getMessage(), e);
        }
      }
    };
  }
}
