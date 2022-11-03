package com.intellij.dmserver.run;

import com.intellij.dmserver.common.ParseUtil;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.javaee.appServers.run.configuration.view.JavaeeRunConfigurationEditorUtil;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.PropertyKey;

import javax.swing.*;

public abstract class DMRunConfigurationEditorBase extends SettingsEditor<CommonModel> {
  private final Project myProject;

  public DMRunConfigurationEditorBase(Project project) {
    myProject = project;
  }

  protected abstract JTextField getJmxPortField();

  protected abstract JTextField getJmxUserField();

  protected abstract JPasswordField getJmxPasswordField();

  protected abstract JLabel getJmxPasswordLabel();

  protected Project getProject() {
    return myProject;
  }

  protected void resetJmxConfiguration(DMServerModelBase serverModel) {
    getJmxPortField().setText(String.valueOf(serverModel.getMBeanServerPort()));
    JavaeeRunConfigurationEditorUtil.resetPasswordFrom(serverModel, getJmxPasswordField(), getJmxPasswordLabel());
    getJmxUserField().setText(notNull(serverModel.getMBeanServerUserName()));
  }

  protected void applyJmxConfiguration(DMServerModelBase serverModel) throws ConfigurationException {
    JavaeeRunConfigurationEditorUtil.applyPasswordTo(serverModel, getJmxPasswordField());
    serverModel.setMBeanServerUserName(getJmxUserField().getText());
    serverModel.setMBeanServerPort(parseInt(getJmxPortField(), "DMRunConfigurationEditorBase.bad.port"));
  }

  protected static int parseInt(JTextField text, final @PropertyKey(resourceBundle = DmServerBundle.BUNDLE) String errorKey)
    throws ConfigurationException {
    return new ParseUtil() {

      @Override
      @Nls
      protected String getErrorMessage(String unparsableValue) {
        return DmServerBundle.message(errorKey, unparsableValue);
      }
    }.parseInt(text);
  }

  protected static String notNull(String s) {
    return s == null ? "" : s;
  }
}