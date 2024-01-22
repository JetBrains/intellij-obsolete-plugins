package com.intellij.plugins.jboss.arquillian.junit.runConfiguration;

import com.google.common.base.Strings;
import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.TestObject;
import com.intellij.execution.junit2.configuration.JUnitConfigurable;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.target.LanguageRuntimeType;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainersManager;
import com.intellij.plugins.jboss.arquillian.runConfiguration.ArquillianRunConfiguration;
import com.intellij.plugins.jboss.arquillian.runConfiguration.ArquillianRunConfigurationCoordinator;
import com.intellij.plugins.jboss.arquillian.runConfiguration.ArquillianRunConfigurationTypeUtil;
import com.intellij.plugins.jboss.arquillian.runConfiguration.ArquillianTestFrameworkRunConfiguration;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ArquillianJUnitRunConfiguration extends JUnitConfiguration implements ArquillianTestFrameworkRunConfiguration {
  private final ArquillianRunConfiguration arquillianRunConfiguration;

  ArquillianJUnitRunConfiguration(String name,
                                  Project project,
                                  @Nls String containerStateName,
                                  ConfigurationFactory configurationFactory) {
    super(name, project, configurationFactory);
    setNameChangedByUser(false);
    this.arquillianRunConfiguration = ArquillianRunConfigurationTypeUtil
      .getInstance(project).createArquillianRunConfiguration(containerStateName);
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

  @Override
  public TestObject getTestObject() {
    return setupRemoteConnection(super.getTestObject());
  }

  @Override
  @Nullable
  public LanguageRuntimeType<?> getDefaultLanguageRuntimeType() {
    return null; // run targets support disabled
  }

  @Override
  public TestObject getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    return setupRemoteConnection(super.getState(executor, env));
  }

  private TestObject setupRemoteConnection(TestObject state) {
    ArquillianRunConfigurationCoordinator coordinator = new ArquillianRunConfigurationCoordinator(getProject());
    state.setRemoteConnectionCreator(coordinator.getRemoteConnectionCreator(arquillianRunConfiguration));
    return state;
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    SettingsEditorGroup<ArquillianJUnitRunConfiguration> group = new SettingsEditorGroup<>();
    group.addEditor(ArquillianBundle.message("arquillian.run.configuration.tab.title"),
                    new ArquillianJUnitConfigurationPanel(getProject()));
    group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"),
                    new JUnitConfigurable<>(getProject()));
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
