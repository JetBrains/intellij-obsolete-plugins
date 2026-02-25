// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.runner;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.GroovyMvcIcons;

import javax.swing.Icon;

public final class GrailsRunConfigurationType implements ConfigurationType {
  private final GrailsConfigurationFactory myConfigurationFactory = new GrailsConfigurationFactory(this);

  @Override
  public @NotNull String getDisplayName() {
    return GrailsBundle.message("library.name");
  }

  @Override
  public String getConfigurationTypeDescription() {
    return GrailsBundle.message("library.name");
  }

  @Override
  public Icon getIcon() {
    return GroovyMvcIcons.Grails;
  }

  @Override
  public @NonNls @NotNull String getId() {
    return "GrailsRunConfigurationType";
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{myConfigurationFactory};
  }

  @Override
  public String getHelpTopic() {
    return "reference.dialogs.rundebug.GrailsRunConfigurationType";
  }

  public static GrailsRunConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(GrailsRunConfigurationType.class);
  }
}
