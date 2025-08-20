package com.intellij.plugins.jboss.arquillian.junit.runConfiguration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.plugins.jboss.arquillian.runConfiguration.ArquillianConfigurationPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class ArquillianJUnitConfigurationPanel extends SettingsEditor<ArquillianJUnitRunConfiguration> {
  private final ArquillianConfigurationPanel underlying;

  ArquillianJUnitConfigurationPanel(Project project) {
    underlying = new ArquillianConfigurationPanel(project);
  }

  @Override
  protected void resetEditorFrom(@NotNull ArquillianJUnitRunConfiguration s) {
    underlying.resetEditorFrom(s.getArquillianRunConfiguration());
  }

  @Override
  protected void applyEditorTo(@NotNull ArquillianJUnitRunConfiguration s) throws ConfigurationException {
    underlying.applyEditorTo(s.getArquillianRunConfiguration());
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return underlying.createEditor();
  }

  @Override
  protected void disposeEditor() {
    super.disposeEditor();
    Disposer.dispose(underlying);
  }
}
