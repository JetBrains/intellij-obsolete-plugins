package com.intellij.plugins.jboss.arquillian.runConfiguration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.WrappingRunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.jarRepository.JarRepositoryManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.MavenManager;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainerState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianExistLibraryState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianLibraryState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianMavenLibraryState;
import org.jetbrains.annotations.NotNull;

public final class ArquillianRunConfigurationTypeUtil implements Disposable {
  @NotNull public static ArquillianRunConfigurationTypeUtil getInstance(Project project) {
    return project.getService(ArquillianRunConfigurationTypeUtil.class);
  }

  @NotNull
  public ArquillianRunConfiguration createArquillianRunConfiguration(@NlsSafe String containerStateName) {
    return new ArquillianRunConfiguration(containerStateName);
  }

  public ArquillianRunConfigurationTypeUtil(@NotNull Project project) {
    project.getMessageBus().connect(this).subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
      @Override
      public void processStarting(@NotNull String executorId, @NotNull final ExecutionEnvironment env) {
        RunProfile runProfile = env.getRunProfile();
        while (runProfile instanceof WrappingRunConfiguration) {
          runProfile = ((WrappingRunConfiguration<?>)runProfile).getPeer();
        }

        if (!(runProfile instanceof ArquillianTestFrameworkRunConfiguration)) {
          return;
        }
        ArquillianRunConfigurationCoordinator coordinator = new ArquillianRunConfigurationCoordinator(env.getProject());
        ArquillianRunConfiguration arquillianRunConfiguration = ((ArquillianTestFrameworkRunConfiguration)runProfile).getRunConfiguration();
        ArquillianContainerState containerState = coordinator.getContainerState(arquillianRunConfiguration);
        if (containerState == null) {
          throw new ProcessCanceledException();
        }
        if (JarRepositoryManager.hasRunningTasks()) {
          ExecutionUtil.handleExecutionError(
            env.getProject(),
            env.getExecutor().getToolWindowId(),
            env.getRunProfile(),
            new ExecutionException(ArquillianBundle.message("arquillian.run.failed.due.to.maven.activity")));
          throw new ProcessCanceledException();
        }


        for (final ArquillianLibraryState library : containerState.libraries) {
          final RunProfile finalRunProfile = runProfile;
          library.accept(new ArquillianLibraryState.Visitor<Void>() {
            @Override
            public Void visitMavenLibrary(ArquillianMavenLibraryState state) {
              try {
                MavenManager.getInstance().getOrLoadMavenArtifactJars(
                  env.getProject(),
                  state.groupId,
                  state.artifactId,
                  state.version,
                  state.downloadSources,
                  state.downloadJavaDocs);
              }
              catch (ExecutionException e) {
                ExecutionUtil.handleExecutionError(
                  env.getProject(),
                  env.getExecutor().getToolWindowId(),
                  finalRunProfile,
                  e);
                throw new ProcessCanceledException(e);
              }
              return null;
            }

            @Override
            public Void visitExistLibrary(ArquillianExistLibraryState state) {
              return null;
            }
          });
        }
      }
    });
  }

  @Override
  public void dispose() { }
}
