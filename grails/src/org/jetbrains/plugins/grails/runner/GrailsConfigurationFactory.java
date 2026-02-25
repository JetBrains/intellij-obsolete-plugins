// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.runner;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.compiler.options.CompileStepBeforeRunNoErrorCheck;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsConstants;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.util.version.Version;

public final class GrailsConfigurationFactory extends ConfigurationFactory {
  GrailsConfigurationFactory(ConfigurationType configurationType) {
    super(configurationType);
  }

  @Override
  public @NotNull String getId() {
    return "Grails";
  }

  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new GrailsRunConfiguration(project, this, GrailsConstants.GRAILS);
  }

  @Override
  public void configureBeforeRunTaskDefaults(Key<? extends BeforeRunTask> providerID, BeforeRunTask task) {
    if (providerID == CompileStepBeforeRun.ID || providerID == CompileStepBeforeRunNoErrorCheck.ID) {
      task.setEnabled(false);
    }
  }

  @Override
  public boolean isApplicable(@NotNull Project project) {
    final GrailsApplicationManager applicationManager = GrailsApplicationManager.getInstance(project);
    return !applicationManager.getApplications().stream()
      .filter(application -> application.getGrailsVersion().isLessThan(Version.GRAILS_6_0))
      .toList().isEmpty();
  }
}
