package com.intellij.plugins.jboss.arquillian.testng.runConfiguration;

import com.google.common.base.Strings;
import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.configurations.*;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainersManager;
import com.intellij.plugins.jboss.arquillian.runConfiguration.ArquillianRunConfiguration;
import com.intellij.plugins.jboss.arquillian.runConfiguration.ArquillianRunConfigurationCoordinator;
import com.intellij.plugins.jboss.arquillian.runConfiguration.ArquillianRunConfigurationTypeUtil;
import com.intellij.plugins.jboss.arquillian.runConfiguration.ArquillianTestFrameworkRunConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class ArquillianTestNGRunConfiguration extends TestNGConfiguration implements ArquillianTestFrameworkRunConfiguration {
  private ArquillianRunConfiguration arquillianRunConfiguration;
  @NotNull private final ArquillianRunConfigurationCoordinator arquillianRunConfigurationCoordinator;

  ArquillianTestNGRunConfiguration(@NotNull Project project, String containerStateName, @NotNull ConfigurationFactory configurationFactory) {
    super(project, configurationFactory);
    setNameChangedByUser(false);
    this.arquillianRunConfiguration = ArquillianRunConfigurationTypeUtil
      .getInstance(project).createArquillianRunConfiguration(containerStateName);
    arquillianRunConfigurationCoordinator = new ArquillianRunConfigurationCoordinator(project);
  }

  @Nullable
  @Override
  public RemoteConnectionCreator getRemoteConnectionCreator() {
    return arquillianRunConfigurationCoordinator.getRemoteConnectionCreator(arquillianRunConfiguration);
  }

  @Override
  public String suggestedName() {
    String containerName = arquillianRunConfiguration.getContainerStateName();
    String suggestedName = super.suggestedName();
    return Strings.isNullOrEmpty(containerName) ? suggestedName : containerName + ": " + suggestedName;
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    super.checkConfiguration();
    String containerStateName = arquillianRunConfiguration.getContainerStateName();
    if (Strings.isNullOrEmpty(containerStateName)) {
      throw new RuntimeConfigurationError(ArquillianBundle.message("arquillian.container.configuration.not.specified"));
    }
    if (ArquillianContainersManager.getInstance(getProject()).findStateByName(containerStateName) == null) {
      throw new RuntimeConfigurationError(ArquillianBundle.message("arquillian.container.configuration.not.found", containerStateName));
    }
  }

  @Nullable
  @Override
  public Set<String> calculateGroupNames() {
    Set<String> groupNames = super.calculateGroupNames();
    if (groupNames == null) {
      return null;
    }
    groupNames.add("arquillian");
    return groupNames;
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    SettingsEditorGroup<ArquillianTestNGRunConfiguration> group = new SettingsEditorGroup<>();
    group.addEditor(ArquillianBundle.message("arquillian.run.configuration.tab.title"),
                    new ArquillianTestNGConfigurationPanel(getProject()));
    group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"),
                    new TestNGConfigurationEditor<>(getProject()));
    JavaRunConfigurationExtensionManager.getInstance().appendEditors(this, group);
    group.addEditor(ExecutionBundle.message("logs.tab.title"), new LogConfigurationPanel<>());
    return group;
  }

  ArquillianRunConfiguration getArquillianRunConfiguration() {
    return arquillianRunConfiguration;
  }

  @Override
  public ArquillianRunConfiguration getRunConfiguration() {
    return arquillianRunConfiguration;
  }
}