package com.intellij.dmserver.run;

import com.intellij.dmserver.integration.DMServerIntegration;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.javaee.appServers.run.configuration.J2EEConfigurationFactory;
import com.intellij.javaee.appServers.run.configuration.JavaeeAppServerConfigurationType;
import com.intellij.javaee.appServers.run.configuration.ServerModel;
import com.intellij.javaee.appServers.run.localRun.ExecutableObjectStartupPolicy;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DMConfigurationFactory extends ConfigurationFactory {
  @Nls
  private final String myName;
  private final String myId;
  private final boolean myIsLocal;

  public DMConfigurationFactory(@NotNull JavaeeAppServerConfigurationType type, @Nls @NotNull String name, @NonNls @NotNull String id, boolean isLocal) {
    super(type);
    myName = name;
    myId = id;
    myIsLocal = isLocal;
  }

  @NotNull
  @Nls
  @Override
  public String getName() {
    return myName;
  }

  @Override
  public @NotNull String getId() {
    return myId;
  }

  @NotNull
  @Override
  public RunConfiguration createConfiguration(@Nullable String name, @NotNull RunConfiguration template) {
    RunConfiguration result = super.createConfiguration(name, template);
    if (result instanceof CommonModel) {
      ((CommonModel)result).initialize();
    }
    return result;
  }

  @NotNull
  @Override
  public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    DMServerIntegration integration = DMServerIntegration.getInstance();
    ServerModel serverModel = createServerModel();

    return J2EEConfigurationFactory.getInstance().createJ2EERunConfiguration(
      this, project, serverModel, integration, myIsLocal, createStartupPolicy());
  }

  protected abstract ServerModel createServerModel();

  protected abstract ExecutableObjectStartupPolicy createStartupPolicy();

  @Override
  public boolean isApplicable(@NotNull Project project) {
    return J2EEConfigurationFactory.getInstance().isConfigurationApplicable((JavaeeAppServerConfigurationType)getType(), project);
  }
}
