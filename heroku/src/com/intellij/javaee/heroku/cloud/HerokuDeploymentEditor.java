package com.intellij.javaee.heroku.cloud;

import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author michael.golubev
 */
public class HerokuDeploymentEditor extends SettingsEditor<HerokuDeploymentConfiguration> {

  private JPanel myMainPanel;
  private JPanel myDeploymentNamePanel;
  private JTextField myDebugPortField;
  private JTextField myHostField;

  private HerokuDeploymentNameEditor myDeploymentNameEditor;

  public HerokuDeploymentEditor() {
    Disposer.register(this, getDeploymentNameEditor());
  }

  @Override
  protected void resetEditorFrom(@NotNull HerokuDeploymentConfiguration settings) {
    myDeploymentNameEditor.resetEditorFrom(settings);
    Integer debugPort = settings.getDebugPort();
    String port = debugPort != null ? String.valueOf(debugPort) : "";
    String host = StringUtil.notNullize(settings.getHost());
    myDebugPortField.setText(port);
    myHostField.setText(host);
  }

  @Override
  protected void applyEditorTo(@NotNull HerokuDeploymentConfiguration settings) throws ConfigurationException {
    myDeploymentNameEditor.applyEditorTo(settings);
    settings.setDebugPort(parsePort());
    settings.setHost(myHostField.getText());
  }

  private void createUIComponents() {
    myDeploymentNamePanel = (JPanel)getDeploymentNameEditor().getComponent();
  }

  private HerokuDeploymentNameEditor getDeploymentNameEditor() {
    if (myDeploymentNameEditor == null) {
      myDeploymentNameEditor = new HerokuDeploymentNameEditor();
    }
    return myDeploymentNameEditor;
  }

  private Integer parsePort() throws RuntimeConfigurationException {
    String debugPort = myDebugPortField.getText();
    if (StringUtil.isEmpty(debugPort)) {
      return null;
    }
    try {
      return Integer.parseInt(debugPort);
    }
    catch (NumberFormatException e) {
      throw new RuntimeConfigurationException("Invalid port number");
    }
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return myMainPanel;
  }
}
