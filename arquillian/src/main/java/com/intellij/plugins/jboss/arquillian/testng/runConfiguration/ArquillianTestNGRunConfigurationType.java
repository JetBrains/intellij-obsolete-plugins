package com.intellij.plugins.jboss.arquillian.testng.runConfiguration;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.ArquillianIcons;
import org.jetbrains.annotations.NotNull;

public final class ArquillianTestNGRunConfigurationType extends ConfigurationTypeBase {
  public ArquillianTestNGRunConfigurationType() {
    super("ArquillianTestNG", ArquillianBundle.message("arquillian.testng.configuration.name"), ArquillianBundle.message("arquillian.testng.configuration.description"),
          NotNullLazyValue.createValue(() -> ArquillianIcons.Arquillian));
    addFactory(new ConfigurationFactory(this) {
      @NotNull
      @Override
      public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new ArquillianTestNGRunConfiguration(project, "", this);
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
  public static ArquillianTestNGRunConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(ArquillianTestNGRunConfigurationType.class);
  }

  @NotNull
  @Override
  public String getTag() {
    return "arquillianTestNg";
  }

  @Override
  public String getHelpTopic() {
    return "reference.dialogs.rundebug.ArquillianTestNG";
  }
}
