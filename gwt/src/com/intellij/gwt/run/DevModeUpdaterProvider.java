package com.intellij.gwt.run;

import com.intellij.compiler.impl.ModuleCompileScope;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.update.RunningApplicationUpdater;
import com.intellij.execution.update.RunningApplicationUpdaterProvider;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.icons.GwtIcons;
import com.intellij.gwt.run.remoteUi.RemoteUiConnection;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public final class DevModeUpdaterProvider implements RunningApplicationUpdaterProvider {
  @Override
  public RunningApplicationUpdater createUpdater(@NotNull Project project, @NotNull ProcessHandler process) {
    final HostedModeWarDirectoryGenerator devModeGenerator = process.getUserData(GwtCommandLineState.GWT_GENERATOR_KEY);
    final GwtCommandLineState state = process.getUserData(GwtCommandLineState.GWT_CONFIGURATION_STATE_KEY);
    if (devModeGenerator != null && state != null) {
      return new DevModeUpdater(project, devModeGenerator, state);
    }
    return null;
  }

  private static class DevModeUpdater implements RunningApplicationUpdater {
    private final Project myProject;
    private final HostedModeWarDirectoryGenerator myDevModeGenerator;
    private final GwtCommandLineState myConfigurationState;

    DevModeUpdater(Project project, HostedModeWarDirectoryGenerator devModeGenerator, GwtCommandLineState state) {
      myProject = project;
      myDevModeGenerator = devModeGenerator;
      myConfigurationState = state;
    }

    @Override
    public String getDescription() {
      return GwtBundle.message("running.app.updater.description.update.0", myConfigurationState.getRunConfigurationName());
    }

    @Override
    public String getShortName() {
      return myConfigurationState.getRunConfigurationName();
    }

    @Override
    public Icon getIcon() {
      return GwtIcons.GoogleSmall;
    }

    @Override
    public void performUpdate(AnActionEvent event) {
      myDevModeGenerator.updateResources(myProject, myConfigurationState.getRunConfigurationName());
      new Task.Backgroundable(myProject, GwtBundle.message("task.title.looking.for.changed.classes"), true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          final ModuleCompileScope scope = new ModuleCompileScope(myConfigurationState.getModule(), false);
          final CompilerManager compilerManager = CompilerManager.getInstance(myProject);
          if (!compilerManager.isUpToDate(scope)) {
            ApplicationManager.getApplication().invokeLater(() ->
              compilerManager.make(myConfigurationState.getModule(), new CompileStatusNotification() {
                @Override
                public void finished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
                  final RemoteUiConnection connection = myConfigurationState.getUiConnection();
                  if (!aborted && errors == 0 && connection != null) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                      myDevModeGenerator.updateResources(myProject, null);
                      connection.sendRestartServerRequest();
                    });
                  }
                }
              })
            );
          }
        }
      }.queue();
    }
  }
}
