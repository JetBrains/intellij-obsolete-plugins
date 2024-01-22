package com.intellij.plugins.jboss.arquillian.configuration.remoteDebugger.ui;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ui.ConfigurationArgumentsHelpArea;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianModel;
import com.intellij.plugins.jboss.arquillian.configuration.remoteDebugger.*;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;

public class RemoteDebuggerPanel extends JPanel {
  private JCheckBox remoteDebugCheckBox;
  private JBRadioButton socketJBRadioButton;
  private JBRadioButton sharedMemoryJBRadioButton;
  private JBRadioButton attachJBRadioButton;
  private JBRadioButton listenJBRadioButton;
  private JPanel settingsPanel;
  private JPanel socketSettingsPanel;
  private JPanel sharedMemorySettingsPanel;
  private JBTextField hostTextField;
  private JBTextField portTextField;
  private JBTextField sharedMemoryAddressTextField;
  private JPanel mainPanel;
  private JBLabel hostLabel;
  private JBLabel portLabel;
  private JBLabel sharedMemoryAddressLabel;
  private ConfigurationArgumentsHelpArea helpArea;
  private ConfigurationArgumentsHelpArea jdk14HelpArea;
  private ConfigurationArgumentsHelpArea jdk13HelpArea;
  private JPanel configurationPanel;

  @NotNull final private RemoteDebuggerModel model;

  public RemoteDebuggerPanel(@NotNull RemoteDebuggerModel model) {
    this.model = model;

    hostLabel.setLabelFor(hostTextField);
    portLabel.setLabelFor(portTextField);
    sharedMemoryAddressLabel.setLabelFor(sharedMemoryAddressTextField);

    hostTextField.setText(model.getHost());
    portTextField.setText(model.getPort());
    sharedMemoryAddressTextField.setText(model.getSharedMemoryAddress());

    helpArea.setLabelText(
      ExecutionBundle.message("remote.configuration.remote.debugging.allows.you.to.connect.idea.to.a.running.jvm.label"));
    helpArea.setToolbarVisible();

    jdk13HelpArea.setLabelText(ExecutionBundle.message("environment.variables.helper.use.arguments.jdk13.label"));
    jdk13HelpArea.setToolbarVisible();
    jdk14HelpArea.setLabelText(ExecutionBundle.message("environment.variables.helper.use.arguments.jdk14.label"));
    jdk14HelpArea.setToolbarVisible();

    configurationPanel.setBorder(IdeBorderFactory.createTitledBorder(ArquillianBundle.message("arquillian.settings.title")));


    modelChanged();
    model.addChangeListener(new ArquillianModel.Listener<>() {
      @Override
      public void itemChanged(RemoteDebuggerModel model) {
        modelChanged();
      }
    });
    remoteDebugCheckBox.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        model.setEnabled(remoteDebugCheckBox.isSelected());
      }
    });
    hostTextField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        model.setHost(hostTextField.getText());
      }
    });
    portTextField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        model.setPort(portTextField.getText());
      }
    });
    sharedMemoryAddressTextField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        model.setSharedMemoryAddress(sharedMemoryAddressTextField.getText());
      }
    });
    socketJBRadioButton.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        model.setTransport(socketJBRadioButton.isSelected() ? RemoteDebuggerTransport.Socket : RemoteDebuggerTransport.SharedMemory);
      }
    });
    sharedMemoryJBRadioButton.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        model.setTransport(socketJBRadioButton.isSelected() ? RemoteDebuggerTransport.Socket : RemoteDebuggerTransport.SharedMemory);
      }
    });
    listenJBRadioButton.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        model.setMode(listenJBRadioButton.isSelected() ? RemoteDebuggerMode.Listen : RemoteDebuggerMode.Attach);
      }
    });
    attachJBRadioButton.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        model.setMode(listenJBRadioButton.isSelected() ? RemoteDebuggerMode.Listen : RemoteDebuggerMode.Attach);
      }
    });
  }

  void modelChanged() {
    setRemoteDebuggerEnabled(model.isEnabled());
    setTransport(model.getTransport());
    setMode(model.getMode());
    RemoteJVMSettingsCoordinator.RemoteJVMSettings remoteJVMSettings = model.getRemoteJVMSettings();
    helpArea.updateText(remoteJVMSettings.jvmSettings);
    jdk13HelpArea.updateText(remoteJVMSettings.jvm13Settings);
    jdk14HelpArea.updateText(remoteJVMSettings.jvm14Settings);
    mainPanel.revalidate();
  }

  private void setRemoteDebuggerEnabled(boolean enabled) {
    remoteDebugCheckBox.setSelected(enabled);
    settingsPanel.setVisible(enabled);
  }

  private void setTransport(RemoteDebuggerTransport transport) {
    if (transport == RemoteDebuggerTransport.Socket) {
      socketJBRadioButton.setSelected(true);
      socketSettingsPanel.setVisible(true);
      sharedMemorySettingsPanel.setVisible(false);
    }
    else {
      sharedMemoryJBRadioButton.setSelected(true);
      sharedMemorySettingsPanel.setVisible(true);
      socketSettingsPanel.setVisible(false);
    }
  }

  private void setMode(RemoteDebuggerMode mode) {
    if (mode == RemoteDebuggerMode.Attach) {
      attachJBRadioButton.setSelected(true);
      hostTextField.setEnabled(true);
    }
    else {
      listenJBRadioButton.setSelected(true);
      hostTextField.setEnabled(false);
    }
  }

  public JPanel getMainPanel() {
    return mainPanel;
  }
}
