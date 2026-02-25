// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.structure.sync;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.compiler.options.CompileStepBeforeRunNoErrorCheck;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration;
import org.jetbrains.plugins.grails.runner.GrailsRunConfigurationType;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

public class GrailsRunConfigurationTask extends GrailsApplicationBackgroundTask {

  public GrailsRunConfigurationTask(Project project) {
    super(project, GrailsBundle.message("progress.title.check.run.configuration"));
  }

  @Override
  protected void run(@NotNull GrailsApplication application, @NotNull ProgressIndicator indicator) {
    if (application.getGrailsVersion().isAtLeast(Version.GRAILS_6_0)) return;

    final GrailsRunConfigurationType configurationType = GrailsRunConfigurationType.getInstance();
    final RunManager runManager = RunManager.getInstance(getProject());
    for (final RunConfiguration runConfiguration : runManager.getConfigurationsList(configurationType)) {
      if (runConfiguration instanceof GrailsRunConfiguration grailsConfiguration) {
        if (grailsConfiguration.getGrailsApplicationNullable() == application) {
          final MvcCommand command = grailsConfiguration.getGrailsCommandNullable();
          if (command != null && "run-app".equals(command.getCommand())) {
            // configuration already exists
            return;
          }
        }
      }
    }
    final ConfigurationFactory factory = configurationType.getConfigurationFactories()[0];
    final RunnerAndConfigurationSettings runSettings = runManager.createConfiguration("Grails: " + application.getName(), factory);
    final GrailsRunConfiguration configuration = (GrailsRunConfiguration)runSettings.getConfiguration();
    configuration.setGrailsApplication(application);
    runManager.addConfiguration(runSettings);
    RunManagerEx.disableTasks(getProject(), configuration, CompileStepBeforeRun.ID, CompileStepBeforeRunNoErrorCheck.ID);
    ApplicationManager.getApplication().invokeLater(() -> runManager.setSelectedConfiguration(runSettings));
  }
}
