package com.intellij.lang.puppet.adapters;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionHelper;
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.ide.IdeCoreBundle;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.project.PuppetEntity;
import com.intellij.lang.puppet.settings.PuppetProjectConfiguration;
import com.intellij.lang.puppet.settings.configurable.PuppetConfigurable;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.intellij.execution.configurations.GeneralCommandLine.ParentEnvironmentType.CONSOLE;

public class PuppetLibrarianAdapter extends PuppetAbstractLibrarianAdapter {

  private static final Logger LOG = Logger.getInstance(PuppetLibrarianAdapter.class);

  @Override
  protected @NotNull Logger getLogger() {
    return LOG;
  }

  @Override
  protected void installDependencies(@NotNull PuppetEntity puppetEntity, @NotNull ProgressIndicator indicator) {
    ReadAction.runBlocking(() -> {
      Project project = puppetEntity.getProject();
      if (project.isDisposed()) {
        return;
      }

      VirtualFile entityRoot = puppetEntity.getRoot();
      if (!entityRoot.isValid()) {
        return;
      }

      List<String> librarianArguments = createLibrarianArguments(puppetEntity);
      if (librarianArguments == null) {
        return;
      }

      GeneralCommandLine commandLine =
        new GeneralCommandLine(ArrayUtilRt.toStringArray(librarianArguments))
          .withParentEnvironmentType(CONSOLE)
          .withWorkDirectory(entityRoot.getCanonicalPath());

      configureEnvironment(commandLine.getEnvironment());

      boolean shouldExcludeLibrarianDir = getLibrarianInternalDir(entityRoot) == null;

      try {
        ProcessHandler processHandler = new KillableColoredProcessHandler(commandLine) {
          @Override
          protected void notifyProcessTerminated(int exitCode) {
            RunContentDescriptor contentDescriptor = RunContentManager.getInstance(project)
              .findContentDescriptor(DefaultRunExecutor.getRunExecutorInstance(), this);

            if (contentDescriptor != null && contentDescriptor.getExecutionConsole() instanceof ConsoleView) {
              ((ConsoleView)contentDescriptor.getExecutionConsole())
                .print("\n" + IdeCoreBundle.message("finished.with.exit.code.text.message", exitCode) + "\n",
                       ConsoleViewContentType.SYSTEM_OUTPUT);
            }
            super.notifyProcessTerminated(exitCode);
          }
        };

        final RunContentExecutor contentExecutor = new RunContentExecutor(project, processHandler)
          .withTitle(PuppetBundle.message("puppet.installing.dependencies", puppetEntity.getDescriptiveName(), puppetEntity.getName()))
          .withFocusToolWindow(false)
          .withAfterCompletion(() -> doPostInstallationWork(puppetEntity, () -> {
            if (shouldExcludeLibrarianDir) {
              VirtualFile librarianInternalDir = getLibrarianInternalDir(entityRoot);
              if (librarianInternalDir != null) {
                excludeVirtualFile(project, librarianInternalDir);
              }
            }
          }));

        ApplicationManager.getApplication().invokeLater(() -> {
          if (!project.isDisposed() && entityRoot.isValid()) {
            contentExecutor.run();
          }
        });
      }
      catch (ExecutionException e) {
        ExecutionHelper
          .showErrors(project, Collections.<Exception>singletonList(e), PuppetBundle.message("puppet.error.running.librarian"), null);
      }
    });
  }

  @Override
  protected @Nullable List<String> createLibrarianArguments(@NotNull PuppetEntity entity) {
    Project project = entity.getProject();
    String librarianPath = PuppetProjectConfiguration.getInstance(project).getLibrarianPath();
    if (StringUtil.isEmpty(librarianPath)) {
      var notification = new Notification(
        "PUPPET_CONFIGURATION_ERROR",
        PuppetBundle.message("puppet.error.librarian.not.configured.title"),
        PuppetBundle.message("puppet.error.librarian.not.configured.message"),
        NotificationType.ERROR);
      notification.addAction(new DumbAwareAction(PuppetBundle.message("puppet.error.librarian.not.configured.action.configure")) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          ShowSettingsUtil.getInstance().editConfigurable(project, new PuppetConfigurable(project));
        }
      });
      Notifications.Bus.notify(notification);
      return null;
    }

    List<String> parentArguments = super.createLibrarianArguments(entity);
    assert parentArguments != null;
    List<String> arguments = new ArrayList<>(parentArguments);
    arguments.add(0, librarianPath);
    return arguments;
  }
}
