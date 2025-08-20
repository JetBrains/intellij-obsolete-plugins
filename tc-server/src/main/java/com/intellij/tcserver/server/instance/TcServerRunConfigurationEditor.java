package com.intellij.tcserver.server.instance;

import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.appServers.run.configuration.ApplicationServerSelectionListener;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.appServers.run.configuration.PredefinedLogFilesListener;
import com.intellij.javaee.appServers.run.configuration.PredefinedLogFilesProviderEditor;
import com.intellij.javaee.appServers.run.configuration.view.JavaeeRunConfigurationEditorUtil;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TcServerRunConfigurationEditor extends SettingsEditor<CommonModel>
  implements ApplicationServerSelectionListener, PredefinedLogFilesProviderEditor, PanelWithAnchor {
  private final EventDispatcher<PredefinedLogFilesListener> myDispatcher = EventDispatcher.create(PredefinedLogFilesListener.class);
  private JPanel myMainPanel;
  private JCheckBox myEnableAuthenticationCheckBox;
  private boolean myIsAuthenticationEnabled;
  private JTextField myLoginField;
  private JPasswordField myPasswordField;
  private JBLabel myPasswordLabel;
  private JComboBox myInstanceModelComboBox;
  private JLabel myInstanceModeLabel;
  private JPanel myInstanceModePanel;
  private final ChangeListener myAuthenticationCheckBoxListener;
  private JComponent anchor;

  public TcServerRunConfigurationEditor() {
    myAuthenticationCheckBoxListener = new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        final boolean isAuthenticationEnabled = myEnableAuthenticationCheckBox.getModel().isSelected();
        if (myIsAuthenticationEnabled != isAuthenticationEnabled) {
          myLoginField.setEnabled(isAuthenticationEnabled);
          myPasswordField.setEnabled(isAuthenticationEnabled);
          myIsAuthenticationEnabled = isAuthenticationEnabled;
        }
      }
    };
    myEnableAuthenticationCheckBox.addChangeListener(myAuthenticationCheckBoxListener);
    myInstanceModePanel.setVisible(SystemInfo.isWindows);
  }

  @Override
  protected void resetEditorFrom(@NotNull CommonModel s) {
    final TcServerModelBase serverModel = (TcServerModelBase)s.getServerModel();
    final boolean isAuthenticationEnabled = serverModel.isJmxAuthenticationEnabled();

    myEnableAuthenticationCheckBox.setSelected(isAuthenticationEnabled);
    myLoginField.setText(serverModel.getLogin());
    JavaeeRunConfigurationEditorUtil.resetPasswordFrom(serverModel, myPasswordField, myPasswordLabel);
    JavaeeRunConfigurationEditorUtil.setReallyEnabled(myPasswordField, isAuthenticationEnabled);
    myInstanceModelComboBox.removeAllItems();

    for (@NlsSafe String item : serverModel.getInstanceModes()) {
      myInstanceModelComboBox.addItem(item);
    }
    myInstanceModelComboBox.setSelectedItem(serverModel.getInstanceMode());
  }

  @Override
  protected void applyEditorTo(@NotNull CommonModel s) {
    final TcServerModelBase serverModel = (TcServerModelBase)s.getServerModel();

    serverModel.setIsJmxAuthenticationEnabled(myEnableAuthenticationCheckBox.getModel().isSelected());
    serverModel.setLogin(myLoginField.getText());
    JavaeeRunConfigurationEditorUtil.applyPasswordTo(serverModel, myPasswordField);
    serverModel.setInstanceMode((String)myInstanceModelComboBox.getSelectedItem());
  }

  @Override
  @NotNull
  protected JComponent createEditor() {
    return myMainPanel;
  }

  @Override
  protected void disposeEditor() {
    for (PredefinedLogFilesListener listener : myDispatcher.getListeners()) {
      myDispatcher.removeListener(listener);
    }
    myEnableAuthenticationCheckBox.removeChangeListener(myAuthenticationCheckBoxListener);
  }

  @Override
  public void addListener(PredefinedLogFilesListener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public void removeListener(PredefinedLogFilesListener listener) {
    myDispatcher.addListener(listener);
  }

  private void refreshLogFiles() {
    try {
      myDispatcher.getMulticaster().predefinedLogFilesChanged(getSnapshot());
    }
    catch (ConfigurationException e) {
      // silently - we can`t get log files from inconsistent installation
    }
  }

  @Override
  public void serverSelected(@Nullable ApplicationServer server) {
    refreshLogFiles();
  }

  @Override
  public void serverProbablyEdited(@Nullable ApplicationServer server) {
    refreshLogFiles();
  }

  @Override
  public JComponent getAnchor() {
    return anchor;
  }

  @Override
  public void setAnchor(JComponent anchor) {
    this.anchor = anchor;
    myPasswordLabel.setAnchor(anchor);
  }

  public void hideInstanceModePanel() {
    myInstanceModePanel.setVisible(false);
  }
}
