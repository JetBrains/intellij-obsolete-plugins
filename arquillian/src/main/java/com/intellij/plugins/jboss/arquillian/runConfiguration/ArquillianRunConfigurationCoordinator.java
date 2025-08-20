package com.intellij.plugins.jboss.arquillian.runConfiguration;

import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RemoteConnectionCreator;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainerState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainersManager;
import com.intellij.plugins.jboss.arquillian.configuration.remoteDebugger.RemoteDebuggerMode;
import com.intellij.plugins.jboss.arquillian.configuration.remoteDebugger.RemoteDebuggerSettingsState;
import com.intellij.plugins.jboss.arquillian.configuration.remoteDebugger.RemoteDebuggerTransport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArquillianRunConfigurationCoordinator {
  @NotNull final private Project project;

  public ArquillianRunConfigurationCoordinator(@NotNull Project project) {
    this.project = project;
  }

  @Nullable
  public ArquillianContainerState getContainerState(ArquillianRunConfiguration runConfiguration) {
    return ArquillianContainersManager
      .getInstance(project)
      .findStateByName(runConfiguration.getContainerStateName());
  }

  @Nullable
  public RemoteConnectionCreator getRemoteConnectionCreator(ArquillianRunConfiguration runConfiguration) {
    ArquillianContainerState containerState = getContainerState(runConfiguration);
    if (containerState == null || !containerState.configurationSpecificState.remoteDebuggingEnabled) {
      return null;
    }
    final RemoteDebuggerSettingsState remoteDebuggerSettings = containerState.configurationSpecificState.settings.clone();
    return new RemoteConnectionCreator() {
      @Override
      public RemoteConnection createRemoteConnection(ExecutionEnvironment environment) {
        return new RemoteConnection(
          remoteDebuggerSettings.transport == RemoteDebuggerTransport.Socket,
          remoteDebuggerSettings.host,
          remoteDebuggerSettings.transport == RemoteDebuggerTransport.Socket
          ? remoteDebuggerSettings.port
          : remoteDebuggerSettings.sharedMemoryAddress,
          remoteDebuggerSettings.mode == RemoteDebuggerMode.Listen);
      }

      @Override
      public boolean isPollConnection() {
        return true;
      }
    };
  }
}
