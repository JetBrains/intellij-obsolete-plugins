package org.intellij.j2ee.web.resin.ui;

import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.JBLabel;
import org.intellij.j2ee.web.resin.ResinBundle;
import org.intellij.j2ee.web.resin.ResinModel;
import org.intellij.j2ee.web.resin.resin.common.ParseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import javax.swing.*;

public class RunConfigurationEditor extends ResinRunConfigurationEditorBase implements PanelWithAnchor {
  private JTextField myHttpPortTextField;
  private JPanel mainPanel;
  private JCheckBox debugConfiguration;
  private TextFieldWithBrowseButton resinConfSelector;
  private JTextField charset;
  private JCheckBox readOnlyConfiguration;
  private RawCommandLineEditor additionalParameters;
  private JCheckBox autoBuildClasspath;
  private JComboBox myDeployModeComboBox;
  private JTextField myJmxPortTextField;
  private JBLabel myAdditionalResinCommandLineLabel;
  private JLabel myJmxPortLabel;
  private JComponent anchor;

  public RunConfigurationEditor() {
    initChooser(resinConfSelector, ResinBundle.message("message.text.settings.resin.conf.file.title"),
                ResinBundle.message("message.text.settings.resin.conf.file.select"));

    myDeployModeComboBox.addItem(ResinModel.DEPLOY_MODE_AUTO);
    myDeployModeComboBox.addItem(ResinModel.DEPLOY_MODE_LAZY);
    myDeployModeComboBox.addItem(ResinModel.DEPLOY_MODE_MANUAL);

    setAnchor(myAdditionalResinCommandLineLabel);
  }

  @Override
  protected void resetEditorFrom(@NotNull CommonModel commonModel) {
    ResinModel resinModel = ((ResinModel)commonModel.getServerModel());
    myHttpPortTextField.setText(String.valueOf(resinModel.getLocalPort()));
    resinConfSelector.setText(resinModel.getResinConf());
    debugConfiguration.setSelected(resinModel.isDebugConfiguration());
    readOnlyConfiguration.setSelected(resinModel.isReadOnlyConfiguration());
    autoBuildClasspath.setSelected(resinModel.isAutoBuildClassPath());
    charset.setText(resinModel.getCharset());
    additionalParameters.setText(resinModel.getAdditionalParameters());

    myJmxPortTextField.setText(String.valueOf(resinModel.getJmxPort()));
    myDeployModeComboBox.setSelectedItem(resinModel.getDeployMode());

    updateJmxPortVisible(resinModel);
  }

  @Override
  protected void applyEditorTo(@NotNull CommonModel commonModel) throws ConfigurationException {
    ResinModel resinModel = ((ResinModel)commonModel.getServerModel());
    resinModel.setPort(parseInt(myHttpPortTextField, "run.config.dlg.http.port.error"));
    resinModel.setResinConf(resinConfSelector.getText());
    resinModel.setDebugConfiguration(debugConfiguration.isSelected());
    resinModel.setReadOnlyConfiguration(readOnlyConfiguration.isSelected());
    resinModel.setAutoBuildClassPath(autoBuildClasspath.isSelected());
    resinModel.setCharset(charset.getText());
    resinModel.setAdditionalParameters(additionalParameters.getText());

    resinModel.setJmxPort(parseInt(myJmxPortTextField, "run.config.dlg.jmx.port.error"));
    resinModel.setDeployMode((String)myDeployModeComboBox.getSelectedItem());
  }

  @Override
  protected void setJmxPortVisible(boolean visible) {
    myJmxPortLabel.setVisible(visible);
    myJmxPortTextField.setVisible(visible);
  }

  @Override
  @NotNull
  protected JComponent createEditor() {
    return mainPanel;
  }

  @Override
  public JComponent getAnchor() {
    return anchor;
  }

  @Override
  public void setAnchor(JComponent anchor) {
    this.anchor = anchor;
    myAdditionalResinCommandLineLabel.setAnchor(anchor);
  }

  private static void initChooser(TextFieldWithBrowseButton field, String title, String description) {
    field.setText("");
    field.getTextField().setEditable(true);
    field.addBrowseFolderListener(title,
                                  description,
                                  null,
                                  FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
  }

  public static int parseInt(JTextField text, final @PropertyKey(resourceBundle = ResinBundle.BUNDLE) String errorKey)
    throws ConfigurationException {
    return new ParseUtil() {

      @Override
      protected String getErrorMessage(String unparsableValue) {
        return ResinBundle.message(errorKey, unparsableValue);
      }
    }.parseInt(text);
  }
}
