package com.intellij.plugins.jboss.arquillian.junit.runConfiguration;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.ArquillianIcons;
import org.jetbrains.annotations.NotNull;

public final class ArquillianJUnitRunConfigurationType extends ConfigurationTypeBase {
  public ArquillianJUnitRunConfigurationType() {
    super("ArquillianJUnit", ArquillianBundle.message("arquillian.junit.configuration.name"), ArquillianBundle.message("arquillian.junit.configuration.description"),
          NotNullLazyValue.createValue(() -> ArquillianIcons.Arquillian));

    addFactory(new ConfigurationFactory(this) {
      @NotNull
      @Override
      public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new ArquillianJUnitRunConfiguration("", project, "", this);
      }

      @NotNull
      @Override
      public String getName() {
        return "";
      }

      @Override
      public @NotNull String getId() {
        return "";
      }
    });
  }

  @NotNull
  public static ArquillianJUnitRunConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(ArquillianJUnitRunConfigurationType.class);
  }

  @NotNull
  @Override
  public String getTag() {
    return "arquillianJunit";
  }

  @Override
  public String getHelpTopic() {
    return "reference.dialogs.rundebug.ArquillianJUnit";
  }
}
