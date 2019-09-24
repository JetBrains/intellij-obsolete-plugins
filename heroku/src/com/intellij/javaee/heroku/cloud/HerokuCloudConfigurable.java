package com.intellij.javaee.heroku.cloud;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.util.CloudConfigurableBase;
import com.intellij.remoteServer.util.ssh.SshKeyChecker;
import com.intellij.ui.HyperlinkLabel;

import javax.swing.*;

/**
 * @author michael.golubev
 */
public class HerokuCloudConfigurable extends CloudConfigurableBase<HerokuCloudConfiguration> {

  private JTextField myEmailTextField;
  private JPasswordField myPasswordField;
  private JPanel myMainPanel;
  private HyperlinkLabel myUploadSshKeyHyperlinkLabel;
  private JPasswordField myApiKeyField;

  public HerokuCloudConfigurable(HerokuCloudConfiguration configuration) {
    super(HerokuCloudType.getInstance(), configuration);
    new SshKeyChecker().setupUploadLabel(myUploadSshKeyHyperlinkLabel, this, myConfiguration, getCloudType());
  }

  @Override
  protected JComponent getMainPanel() {
    return myMainPanel;
  }

  @Override
  protected JTextField getEmailTextField() {
    return myEmailTextField;
  }

  @Override
  protected JPasswordField getPasswordField() {
    return myPasswordField;
  }

  @Override
  public boolean isModified() {
    return super.isModified() ||
           !new String(myApiKeyField.getPassword()).equals(myConfiguration.getApiKeySafe()) ||
           !myConfiguration.isApiKeySafe();
  }

  @Override
  public void reset() {
    super.reset();
    myApiKeyField.setText(myConfiguration.getApiKeySafe());
  }

  @Override
  protected void applyCoreTo(HerokuCloudConfiguration configuration, boolean forComparison) throws ConfigurationException {
    super.applyCoreTo(configuration, forComparison);

    String apiKey = new String(myApiKeyField.getPassword());
    if (StringUtil.isEmpty(apiKey)) {
      throw new RuntimeConfigurationError("API key required");
    }
    if (forComparison) {
      configuration.setApiKey(apiKey);
    }
    else {
      configuration.setApiKeySafe(apiKey);
    }
  }
}
