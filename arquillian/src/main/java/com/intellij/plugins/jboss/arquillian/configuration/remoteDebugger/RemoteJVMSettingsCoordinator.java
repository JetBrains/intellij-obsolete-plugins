package com.intellij.plugins.jboss.arquillian.configuration.remoteDebugger;

import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class RemoteJVMSettingsCoordinator {
  private final Map<RemoteDebuggerSettingsState, RemoteJVMSettings> results = new HashMap<>();
  @NotNull private final Callback callback;
  @NotNull private final Project project;

  @NotNull public static final RemoteJVMSettings inProgressStateSettings = new RemoteJVMSettings(
    "Evaluating...",
    "Evaluating...",
    "Evaluating...");

  public RemoteJVMSettingsCoordinator(@NotNull Project project, @NotNull Callback callback) {
    this.callback = callback;
    this.project = project;
  }

  public void retrieveSettingsForState(final RemoteDebuggerSettingsState state) {
    RemoteJVMSettings settings = results.get(state);
    if (settings == null) {
      settings = inProgressStateSettings;
      results.put(state, settings);
      //noinspection DialogTitleCapitalization
      ApplicationManager.getApplication().invokeLater(() ->
                                                        new Task.Backgroundable(project,
                                                                                ArquillianBundle.message("arquillian.jvm.args.retrieve"),
                                                                                true) {
                                                          @Override
                                                          public void run(@NotNull ProgressIndicator indicator) {
                                                            final RemoteJVMSettings settings = doRetrieveRemoteJVMSettings(state);
                                                            //SwingUtilities.invokeLater(
                                                            ApplicationManager.getApplication().invokeLater(
                                                              () -> {
                                                                results.put(state, settings);
                                                                callback.settingsForState(state, settings);
                                                              }, ModalityState.any());
                                                          }
                                                        }.queue(), ModalityState.any());
    }
    callback.settingsForState(state, settings);
  }

  private static RemoteJVMSettings doRetrieveRemoteJVMSettings(RemoteDebuggerSettingsState state) {
    boolean useSockets = state.transport == RemoteDebuggerTransport.Socket;
    final RemoteConnection connection = new RemoteConnection(
      useSockets,
      state.host,
      useSockets ? state.port.trim() : state.sharedMemoryAddress.trim(),
      state.mode == RemoteDebuggerMode.Listen);
    final String cmdLine = connection.getLaunchCommandLine();
    // -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=7007
    final String jvmtiCmdLine = cmdLine.replace("-Xdebug", "").replace("-Xrunjdwp:", "-agentlib:jdwp=").trim();
    return new RemoteJVMSettings(
      "-Xnoagent -Djava.compiler=NONE " + cmdLine,
      cmdLine,
      jvmtiCmdLine);
  }

  public interface Callback {
    void settingsForState(RemoteDebuggerSettingsState state, RemoteJVMSettings settings);
  }

  public static class RemoteJVMSettings {
    @NotNull public final String jvmSettings;
    @NotNull public final String jvm14Settings;
    @NotNull public final String jvm13Settings;

    public RemoteJVMSettings(@NotNull String jvm13Settings, @NotNull String jvm14Settings, @NotNull String jvmSettings) {
      this.jvm13Settings = jvm13Settings;
      this.jvm14Settings = jvm14Settings;
      this.jvmSettings = jvmSettings;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      RemoteJVMSettings settings = (RemoteJVMSettings)o;

      if (!jvmSettings.equals(settings.jvmSettings)) return false;
      if (!jvm14Settings.equals(settings.jvm14Settings)) return false;
      if (!jvm13Settings.equals(settings.jvm13Settings)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = jvmSettings.hashCode();
      result = 31 * result + jvm14Settings.hashCode();
      result = 31 * result + jvm13Settings.hashCode();
      return result;
    }
  }
}
