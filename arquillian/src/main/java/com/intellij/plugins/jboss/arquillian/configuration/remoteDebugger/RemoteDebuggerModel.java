package com.intellij.plugins.jboss.arquillian.configuration.remoteDebugger;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianModel;
import org.jetbrains.annotations.NotNull;

public class RemoteDebuggerModel extends ArquillianModel<ConfigurationSpecificState, RemoteDebuggerModel>
  implements RemoteJVMSettingsCoordinator.Callback {
  @NotNull final private RemoteJVMSettingsCoordinator remoteJVMSettingsCoordinator;
  @NotNull private final ConfigurationSpecificState currentState;
  @NotNull private RemoteJVMSettingsCoordinator.RemoteJVMSettings remoteJVMSettings = RemoteJVMSettingsCoordinator.inProgressStateSettings;

  public RemoteDebuggerModel(Project project, @NotNull ConfigurationSpecificState state) {
    remoteJVMSettingsCoordinator = new RemoteJVMSettingsCoordinator(project, this);
    this.currentState = state.clone();
    remoteJVMSettingsCoordinator.retrieveSettingsForState(currentState.settings.clone());
    addChangeListener(new Listener<>() {
      @Override
      public void itemChanged(RemoteDebuggerModel model) {
        remoteJVMSettingsCoordinator.retrieveSettingsForState(currentState.settings.clone());
      }
    });
  }

  @NotNull
  public RemoteJVMSettingsCoordinator.RemoteJVMSettings getRemoteJVMSettings() {
    return remoteJVMSettings;
  }

  @Override
  public boolean hasChanges(ConfigurationSpecificState state) {
    return !currentState.equals(state);
  }

  @NotNull
  @Override
  public ConfigurationSpecificState getCurrentState() {
    return currentState.clone();
  }

  public boolean isEnabled() {
    return currentState.remoteDebuggingEnabled;
  }

  public void setEnabled(boolean enabled) {
    if (currentState.remoteDebuggingEnabled != enabled) {
      currentState.remoteDebuggingEnabled = enabled;
      notifyMeChanged();
    }
  }

  public String getHost() {
    return currentState.settings.host;
  }

  public void setHost(String host) {
    if (!currentState.settings.host.equals(host)) {
      currentState.settings.host = host;
      notifyMeChanged();
    }
  }

  public RemoteDebuggerMode getMode() {
    return currentState.settings.mode;
  }

  public void setMode(RemoteDebuggerMode mode) {
    if (currentState.settings.mode != mode) {
      currentState.settings.mode = mode;
      notifyMeChanged();
    }
  }

  public String getPort() {
    return currentState.settings.port;
  }

  public void setPort(String port) {
    if (!currentState.settings.port.equals(port)) {
      currentState.settings.port = port;
      notifyMeChanged();
    }
  }

  public String getSharedMemoryAddress() {
    return currentState.settings.sharedMemoryAddress;
  }

  public void setSharedMemoryAddress(String sharedMemoryAddress) {
    if (!currentState.settings.sharedMemoryAddress.equals(sharedMemoryAddress)) {
      currentState.settings.sharedMemoryAddress = sharedMemoryAddress;
      notifyMeChanged();
    }
  }

  public RemoteDebuggerTransport getTransport() {
    return currentState.settings.transport;
  }

  public void setTransport(RemoteDebuggerTransport transport) {
    if (currentState.settings.transport != transport) {
      currentState.settings.transport = transport;
      notifyMeChanged();
    }
  }

  public String getRunContainerQualifier() {
    return currentState.runContainerQualifier;
  }

  public void setRunContainerQualifier(String containerQualifier) {
    if (!currentState.runContainerQualifier.equals(containerQualifier)) {
      currentState.runContainerQualifier = containerQualifier;
      notifyMeChanged();
    }
  }

  public String getDebugContainerQualifier() {
    return currentState.debugContainerQualifier;
  }

  public void setDebugContainerQualifier(String containerQualifier) {
    if (!currentState.debugContainerQualifier.equals(containerQualifier)) {
      currentState.debugContainerQualifier = containerQualifier;
      notifyMeChanged();
    }
  }

  @Override
  public void settingsForState(RemoteDebuggerSettingsState state, RemoteJVMSettingsCoordinator.RemoteJVMSettings settings) {
    if (!currentState.settings.equals(state)) {
      return;
    }
    if (remoteJVMSettings.equals(settings)) {
      return;
    }
    remoteJVMSettings = settings;
    notifyMeChanged();
  }
}
