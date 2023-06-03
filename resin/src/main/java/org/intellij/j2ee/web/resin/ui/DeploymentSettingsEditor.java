package org.intellij.j2ee.web.resin.ui;

import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.appServers.deployment.DeploymentSource;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import org.intellij.j2ee.web.resin.ResinModuleDeploymentModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeploymentSettingsEditor extends SettingsEditor<DeploymentModel> {

  private JPanel myMainPanel;

  private JTextField myHostField;
  private JTextField myApplicationContextField;
  private JCheckBox myDefaultContextCheckBox;

  public DeploymentSettingsEditor(final CommonModel commonModel, final DeploymentSource deploymentSource) {
    super(() -> new ResinModuleDeploymentModel(commonModel, deploymentSource));
    myDefaultContextCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateContextEnabled();
      }
    });
    myDefaultContextCheckBox.setSelected(true);
    updateContextEnabled();
  }

  private void updateContextEnabled() {
    myApplicationContextField.setEnabled(!myDefaultContextCheckBox.isSelected());
  }

  @Override
  public void resetEditorFrom(@NotNull DeploymentModel settings) {
    ResinModuleDeploymentModel resinDeploymentModel = (ResinModuleDeploymentModel)settings;
    myDefaultContextCheckBox.setSelected(resinDeploymentModel.isDefaultContextPath());
    updateContextEnabled();
    myApplicationContextField.setText(resinDeploymentModel.getContextPath());
    myHostField.setText(resinDeploymentModel.getHost());
  }

  @Override
  public void applyEditorTo(@NotNull DeploymentModel settings) throws ConfigurationException {
    ResinModuleDeploymentModel resinDeploymentModel = (ResinModuleDeploymentModel)settings;
    resinDeploymentModel.setDefaultContextPath(myDefaultContextCheckBox.isSelected());
    resinDeploymentModel.setContextPath(myApplicationContextField.getText());
    resinDeploymentModel.setHost(myHostField.getText());
  }

  @Override
  @NotNull
  protected JComponent createEditor() {
    return myMainPanel;
  }
}
