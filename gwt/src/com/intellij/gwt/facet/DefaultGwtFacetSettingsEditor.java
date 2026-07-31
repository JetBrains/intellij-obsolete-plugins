package com.intellij.gwt.facet;

import com.intellij.facet.ui.DefaultFacetSettingsEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.GwtJavaScriptOutputStyle;

import javax.swing.JComponent;

public class DefaultGwtFacetSettingsEditor extends DefaultFacetSettingsEditor {
  private final GwtFacetConfiguration myConfiguration;
  private final GwtFacetCommonSettingsPanel myPanel;

  public DefaultGwtFacetSettingsEditor(final @NotNull Project project, @NotNull GwtFacetConfiguration configuration) {
    myConfiguration = configuration;
    myPanel = new GwtFacetCommonSettingsPanel(project);
  }

  @Override
  public JComponent createComponent() {
    return myPanel.getMainPanel();
  }

  @Override
  public void reset() {
    myPanel.getSdkPathEditor().setPath(myConfiguration.getGwtSdkPath());
    myPanel.getOutputStyleComboBox().setSelectedItem(myConfiguration.getOutputStyle());
    myPanel.getCompilerHeapSizeField().setText(String.valueOf(myConfiguration.getCompilerMaxHeapSize()));
  }

  @Override
  public void apply() throws ConfigurationException {
    myConfiguration.setGwtSdkUrlAndType(myPanel.getSdkPathEditor().getUrl());
    myConfiguration.setOutputStyle(getOutputStyle());
    try {
      myConfiguration.setCompilerMaxHeapSize(Integer.parseInt(myPanel.getCompilerHeapSizeField().getText()));
    }
    catch (NumberFormatException ignored) {
    }
  }

  @Override
  public boolean isModified() {
    return !myConfiguration.getGwtSdkPath().equals(myPanel.getSdkPathEditor().getPath()) ||
           !myConfiguration.getOutputStyle().equals(getOutputStyle()) ||
           !String.valueOf(myConfiguration.getCompilerMaxHeapSize()).equals(myPanel.getCompilerHeapSizeField().getText());
  }

  private GwtJavaScriptOutputStyle getOutputStyle() {
    return (GwtJavaScriptOutputStyle)myPanel.getOutputStyleComboBox().getSelectedItem();
  }
}
