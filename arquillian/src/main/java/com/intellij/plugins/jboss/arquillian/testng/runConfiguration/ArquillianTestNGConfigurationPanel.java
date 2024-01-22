package com.intellij.plugins.jboss.arquillian.testng.runConfiguration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.plugins.jboss.arquillian.runConfiguration.ArquillianConfigurationPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class ArquillianTestNGConfigurationPanel extends SettingsEditor<ArquillianTestNGRunConfiguration> {
  private final ArquillianConfigurationPanel underlying;

  ArquillianTestNGConfigurationPanel(Project project) {
    underlying = new ArquillianConfigurationPanel(project);
  }

  @Override
  public void resetEditorFrom(@NotNull ArquillianTestNGRunConfiguration s) {
    underlying.resetEditorFrom(s.getArquillianRunConfiguration());
  }

  @Override
  public void applyEditorTo(@NotNull ArquillianTestNGRunConfiguration s) throws ConfigurationException {
    underlying.applyEditorTo(s.getArquillianRunConfiguration());
  }

  @NotNull
  @Override
  public JComponent createEditor() {
    return underlying.createEditor();
  }

  @Override
  protected void disposeEditor() {
    super.disposeEditor();
    Disposer.dispose(underlying);
  }
}

