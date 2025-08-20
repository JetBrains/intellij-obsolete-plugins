package com.intellij.tcserver.server.instance.remote;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.transport.TransportManager;
import com.intellij.javaee.transport.TransportTarget;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.Disposer;
import com.intellij.tcserver.server.instance.TcServerRunConfigurationEditor;
import com.intellij.tcserver.util.TcServerBundle;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class TcRemoteServerRunConfigutationEditor extends SettingsEditor<CommonModel> implements PanelWithAnchor {
  private JPanel myMainPanel;
  private TcServerRunConfigurationEditor myCommonConfigurationEditor;
//  private TransportTargetConfigurable myTransportTargetConfigurable; // todo cannot compile form outside of IJ repository
//  private TransportManagerConfigurable myTransportManagerConfigurable; // todo cannot compile form outside of IJ repository
  private JTextField myStagingPathField;
  private JTextField myJmxPortField;
  private JBLabel myJmxPortLabel;
  private JComponent anchor;

  private TransportTarget myTransportTarget;

  public TcRemoteServerRunConfigutationEditor() {
//    myTransportTargetConfigurable.setParentConfigurable(myTransportManagerConfigurable);

    setAnchor(myJmxPortLabel);
    myCommonConfigurationEditor.hideInstanceModePanel();
  }

  @Override
  public JComponent getAnchor() {
    return anchor;
  }

  @Override
  public void setAnchor(JComponent anchor) {
    this.anchor = anchor;
    myJmxPortLabel.setAnchor(anchor);
    myCommonConfigurationEditor.setAnchor(anchor);
  }

  private static String notNull(String s) {
    return s == null ? "" : s;
  }

  @Override
  protected void resetEditorFrom(@NotNull CommonModel s) {
    myCommonConfigurationEditor.resetFrom(s);

    TcServerRemoteModel serverModel = (TcServerRemoteModel)s.getServerModel();

    myStagingPathField.setText(notNull(serverModel.getStagingRemotePath()));

//    myTransportManagerConfigurable.setHostId(serverModel.getTransportHostId(), serverModel.getProject());
    myTransportTarget = getOrCreateTransportTarget(serverModel.getTransportStagingTarget());
//    myTransportTargetConfigurable.setTarget(myTransportTarget);

    myJmxPortField.setText(String.valueOf(serverModel.getJmxPort()));
  }

  private static TransportTarget getOrCreateTransportTarget(TransportTarget target) {
    return target == null || target.getId() == null ? TransportManager.createTarget() : target;
  }

  @Override
  protected void applyEditorTo(@NotNull CommonModel s) throws ConfigurationException {
    myCommonConfigurationEditor.applyTo(s);

    TcServerRemoteModel serverModel = (TcServerRemoteModel)s.getServerModel();

    serverModel.setStagingRemotePath(myStagingPathField.getText());

//    serverModel.setTransportHostId(myTransportManagerConfigurable.getHostId());
    serverModel.setTransportStagingTarget(myTransportTarget);
//    myTransportTargetConfigurable.saveState();

    try {
      serverModel.setJmxPort(Integer.parseInt(myJmxPortField.getText()));
    }
    catch (NumberFormatException e) {
      throw new RuntimeConfigurationError(TcServerBundle.message("runConfigurationEditor.invalidPort", myJmxPortField.getText()));
    }

    TcServerRemoteModel.validate(serverModel);
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return myMainPanel;
  }

  @Override
  protected void disposeEditor() {
    Disposer.dispose(myCommonConfigurationEditor);
  }
}
