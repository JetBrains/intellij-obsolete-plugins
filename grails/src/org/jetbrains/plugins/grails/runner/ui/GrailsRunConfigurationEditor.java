// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner.ui;

import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class GrailsRunConfigurationEditor extends GrailsRunConfigurationEditorWithListener {

  private JPanel myMainPanel;

  private JBLabel myApplicationLabel;
  private GrailsApplicationCombobox myApplicationsCombo;

  private JBLabel myCommandLineLabel;
  private JTextField myCommandLine;

  private JBLabel myVMParametersLabel;
  private RawCommandLineEditor myVMParameters;

  private EnvironmentVariablesComponent myEnvVariablesComponent;

  private JBCheckBox myLaunchBrowser;
  private JBTextField myLaunchBrowserUrl;

  private JBLabel myOptionsLabel;
  private JPanel myExtensionPanel;
  private final Collection<SettingsEditor<GrailsRunConfiguration>> myExtensionEditors = new ArrayList<>();

  public GrailsRunConfigurationEditor(Project project) {
    myApplicationsCombo.setApplications(GrailsApplicationManager.getInstance(project).getApplications());
    myApplicationsCombo.addItemListener(e -> applicationChanged(getApplication()));
    myCommandLine.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        commandLineChanged(getCommandLine());
      }
    });
    myVMParameters.setDialogCaption("VM Options");
    myLaunchBrowser.addChangeListener(e -> myLaunchBrowserUrl.setEnabled(myLaunchBrowser.isEnabled() && myLaunchBrowser.isSelected()));
    setAnchor();
  }

  private void setAnchor() {
    double maxWidth = -1;
    JComponent anchor = null;
    JBLabel[] components = ContainerUtil.ar(
      myApplicationLabel, myCommandLineLabel, myVMParametersLabel, myEnvVariablesComponent.getLabel()
    );
    for (JComponent candidate : components) {
      double width = candidate.getPreferredSize().getWidth();
      if (width > maxWidth) {
        maxWidth = width;
        anchor = candidate;
      }
    }
    if (anchor != null) {
      for (JBLabel component : components) {
        component.setAnchor(anchor);
      }
    }
  }

  @Override
  protected void resetEditorFrom(@NotNull GrailsRunConfiguration configuration) {
    GrailsApplication application = configuration.getGrailsApplicationNullable();
    myApplicationsCombo.setSelectedApplication(application);
    if (application == null) applicationChanged(null);
    myCommandLine.setText(configuration.getProgramParameters());
    myVMParameters.setText(configuration.getVMParameters());
    myEnvVariablesComponent.setEnvs(new HashMap<>(configuration.getEnvs()));
    myEnvVariablesComponent.setPassParentEnvs(configuration.isPassParentEnvs());
    myLaunchBrowser.setSelected(configuration.isLaunchBrowser());
    myLaunchBrowserUrl.setText(configuration.getLaunchBrowserUrl());
    myExtensionEditors.forEach(e -> e.resetFrom(configuration));
  }

  @Override
  protected void applyEditorTo(@NotNull GrailsRunConfiguration configuration) throws ConfigurationException {
    configuration.setGrailsApplication(getApplication());
    configuration.setVMParameters(myVMParameters.getText().trim());
    configuration.setProgramParameters(myCommandLine.getText().trim());
    configuration.setEnvs(myEnvVariablesComponent.getEnvs());
    configuration.setPassParentEnvs(myEnvVariablesComponent.isPassParentEnvs());
    configuration.setLaunchBrowser(myLaunchBrowser.isSelected());
    configuration.setLaunchBrowserUrl(myLaunchBrowserUrl.getText());
    for (SettingsEditor<GrailsRunConfiguration> editor : myExtensionEditors) {
      editor.applyTo(configuration);
    }
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return myMainPanel;
  }

  @Override
  public void applicationChanged(GrailsApplication application) {
    for (SettingsEditor<GrailsRunConfiguration> editor : myExtensionEditors) {
      if (editor instanceof GrailsRunConfigurationEditorListener) {
        ((GrailsRunConfigurationEditorListener)editor).applicationChanged(application);
      }
    }
    checkExtensionPanel();
  }

  @Override
  public void commandLineChanged(String commandLine) {
    setCBEnabled("run-app".equals(MvcCommand.parse(commandLine).getCommand()), myLaunchBrowser);
    for (SettingsEditor<GrailsRunConfiguration> editor : myExtensionEditors) {
      if (editor instanceof GrailsRunConfigurationEditorListener) {
        ((GrailsRunConfigurationEditorListener)editor).commandLineChanged(commandLine);
      }
    }
    checkExtensionPanel();
  }

  private void checkExtensionPanel() {
    boolean hasActiveExtensionEditors = hasActiveExtensionEditors();
    myOptionsLabel.setVisible(hasActiveExtensionEditors);
    myExtensionPanel.setVisible(hasActiveExtensionEditors);
  }

  private boolean hasActiveExtensionEditors() {
    for (Component component : myExtensionPanel.getComponents()) {
      if (component.isVisible()) {
        return true;
      }
    }
    return false;
  }

  public void addExtension(@NotNull SettingsEditor<GrailsRunConfiguration> editor) {
    Disposer.register(this, editor);
    myExtensionEditors.add(editor);
    myExtensionPanel.add(editor.getComponent(), BorderLayout.PAGE_START);
  }

  private @Nullable GrailsApplication getApplication() {
    return myApplicationsCombo.getSelectedApplication();
  }

  private @NotNull String getCommandLine() {
    final String commandLine = StringUtil.trim(myCommandLine.getText());
    return commandLine == null ? "" : commandLine;
  }

  public static void setCBEnabled(boolean enabled, final JCheckBox checkBox) {
    final boolean wasEnabled = checkBox.isEnabled();
    checkBox.setEnabled(enabled);
    if (wasEnabled && !enabled) {
      checkBox.setSelected(false);
    }
    else if (!wasEnabled && enabled) {
      checkBox.setSelected(true);
    }
  }
}
