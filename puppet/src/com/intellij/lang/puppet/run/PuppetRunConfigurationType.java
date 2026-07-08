package com.intellij.lang.puppet.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.PuppetIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Anna Bulenkova
 *         Under construction, disabled.
 */
public final class PuppetRunConfigurationType extends ConfigurationTypeBase {
  private PuppetRunConfigurationType() {
    super("PuppetRunConfigurationType",
          PuppetBundle.message("run.configuration.name"),
          PuppetBundle.message("run.configuration.description"),
          PuppetIcons.PuppetLogo);
    addFactory(new PuppetRunConfigurationFactory(this));
  }

  @Override
  public String getHelpTopic() {
    return "reference.dialogs.rundebug.PuppetRunConfigurationType";
  }

  public static PuppetRunConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(PuppetRunConfigurationType.class);
  }

  public static class PuppetRunConfigurationFactory extends ConfigurationFactory {
    protected PuppetRunConfigurationFactory(final PuppetRunConfigurationType type) {
      super(type);
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(final @NotNull Project project) {
      return new PuppetRunConfiguration(project, this, "Puppet");
    }

    @Override
    public @NotNull String getId() {
      return "Application";
    }
  }
}
