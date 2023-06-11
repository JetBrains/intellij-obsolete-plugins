package com.intellij.tcserver.deployment;

import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AdditionalDeploymentSettingsEditor extends SettingsEditor<DeploymentModel> {
  private JPanel myMainPanel;
  private JTextField myWebPathField;
  private JTextField myServerHostNameField;
  private JTextField myServerServiceField;

  @Override
  protected void resetEditorFrom(@NotNull DeploymentModel depModel) {
    TcServerDeploymentModel deploymentModel = (TcServerDeploymentModel)depModel;
    myWebPathField.setText(deploymentModel.getWebPath());
    myServerHostNameField.setText(deploymentModel.getServerHost());
    myServerServiceField.setText(deploymentModel.getServerService());
  }

  @Override
  protected void applyEditorTo(@NotNull DeploymentModel depModel) throws ConfigurationException {
    TcServerDeploymentModel deploymentModel = (TcServerDeploymentModel)depModel;
    String webPath = TcServerDeploymentModel.validateWebPath(myWebPathField.getText());
    deploymentModel.setWebPath(webPath);
    deploymentModel.setServerHost(myServerHostNameField.getText());
    deploymentModel.setServerService(myServerServiceField.getText());
  }


  @NotNull
  @Override
  protected JComponent createEditor() {
    return myMainPanel;
  }
}
