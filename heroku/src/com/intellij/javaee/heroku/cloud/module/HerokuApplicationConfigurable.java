package com.intellij.javaee.heroku.cloud.module;

import com.intellij.javaee.heroku.cloud.HerokuAppTemplate;
import com.intellij.javaee.heroku.cloud.HerokuCloudConfiguration;
import com.intellij.javaee.heroku.cloud.HerokuDeploymentConfiguration;
import com.intellij.javaee.heroku.cloud.HerokuServerRuntimeInstance;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.remoteServer.impl.module.CloudSourceApplicationConfigurable;
import com.intellij.ui.components.JBRadioButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author michael.golubev
 */
public class HerokuApplicationConfigurable extends CloudSourceApplicationConfigurable<
  HerokuCloudConfiguration, HerokuDeploymentConfiguration, HerokuServerRuntimeInstance, HerokuApplicationConfiguration> {

  private static final HerokuAppTemplate[] APP_TEMPLATES = new HerokuAppTemplate[]{
    new HerokuAppTemplate("java-sample"),
    new HerokuAppTemplate("java-spring-sample")
  };

  private JPanel myMainPanel;
  private ComboBox myTemplateComboBox;
  private JBRadioButton myTemplateRadioButton;
  private JBRadioButton myExistingRadioButton;
  private ComboBox myExistingComboBox;

  public HerokuApplicationConfigurable(Project project, Disposable parentDisposable) {
    super(project, parentDisposable);

    ActionListener updateApplicationListener = new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        updateApplicationUI();
      }
    };
    myTemplateRadioButton.addActionListener(updateApplicationListener);
    myExistingRadioButton.addActionListener(updateApplicationListener);
    myExistingComboBox.addActionListener(updateApplicationListener);

    for (HerokuAppTemplate appTemplate : APP_TEMPLATES) {
      myTemplateComboBox.addItem(appTemplate);
    }
    myTemplateRadioButton.setSelected(true);
  }

  private void updateApplicationUI() {
    myTemplateComboBox.setEnabled(myTemplateRadioButton.isSelected());
    boolean isExisting = myExistingRadioButton.isSelected();
    myExistingComboBox.setEnabled(isExisting);
  }

  @Override
  protected JComboBox getExistingComboBox() {
    return myExistingComboBox;
  }

  @Override
  public JComponent getMainPanel() {
    return myMainPanel;
  }

  @Override
  public HerokuApplicationConfiguration createConfiguration() {
    return new HerokuApplicationConfiguration(
      myTemplateRadioButton.isSelected(),
      (HerokuAppTemplate)myTemplateComboBox.getSelectedItem(),
      myExistingRadioButton.isSelected(),
      (String)myExistingComboBox.getSelectedItem());
  }

  @Override
  public void validate() throws ConfigurationException {
    if (myExistingRadioButton.isSelected() && myExistingComboBox.getSelectedItem() == null) {
      throw new ConfigurationException("Existing application should be chosen");
    }
  }
}
