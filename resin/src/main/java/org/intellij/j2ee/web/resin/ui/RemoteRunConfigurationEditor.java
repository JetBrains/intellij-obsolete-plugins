package org.intellij.j2ee.web.resin.ui;

import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.transport.TransportManager;
import com.intellij.javaee.transport.TransportManagerConfigurable;
import com.intellij.javaee.transport.TransportTarget;
import com.intellij.javaee.transport.TransportTargetConfigurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import org.intellij.j2ee.web.resin.ResinBundle;
import org.intellij.j2ee.web.resin.ResinRemoteModel;
import org.intellij.j2ee.web.resin.resin.jmx.ConnectorPingCommand;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RemoteRunConfigurationEditor extends ResinRunConfigurationEditorBase {

  private JPanel myRootPanel;

  private JTextField myJmxPortField;
  private JButton myPingButton;
//  todo cannot be migrated due to .form compilation errors
//  private TransportManagerConfigurable myTransportManagerConfigurable;
//  private TransportTargetConfigurable myDeploymentTransportTargetConfigurable;
  private JTextField myCharsetField;
  private JLabel myJmxPortLabel;

  private final Project myProject;

  private ResinRemoteModel myServerModel;

  private TransportTarget myDeploymentTransportTarget;

  public RemoteRunConfigurationEditor(@NotNull Project project) {
    myProject = project;

    myPingButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onPingButton();
      }
    });

//    myDeploymentTransportTargetConfigurable.setParentConfigurable(myTransportManagerConfigurable);
  }

  private Project getProject() {
    return myProject;
  }

  // TODO: copy-pasted from DM
  private void onPingButton() {

    final int jmxPort;
    try {
      jmxPort = parseJmxPort();
    }
    catch (ConfigurationException e) {
      return;
    }


    final Ref<Boolean> success = new Ref<>(null);
    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      try {
        Boolean pingResult = new ConnectorPingCommand(myServerModel, jmxPort).execute();
        success.set(pingResult != null && pingResult.booleanValue());
      }
      catch (Exception ex) {
        success.set(false);
      }
    }, ResinBundle.message("RemoteRunConfigurationEditor.message.ping.operation-name"), true, getProject())) {
      return;
    }

    if (success.isNull()) {
      return;
    }

    String ping = ResinBundle.message("RemoteRunConfigurationEditor.message.ping");
    if (success.get()) {
      Messages.showInfoMessage(ResinBundle.message("RemoteRunConfigurationEditor.message.ping.ok"), ping);
    }
    else {
      Messages.showErrorDialog(ResinBundle.message("RemoteRunConfigurationEditor.message.ping.failed"), ping);
    }
  }

  private int parseJmxPort() throws ConfigurationException {
    return RunConfigurationEditor.parseInt(myJmxPortField, "run.config.dlg.jmx.port.error");
  }

  @Override
  protected void resetEditorFrom(@NotNull CommonModel s) {
    myServerModel = ((ResinRemoteModel)s.getServerModel());

    myCharsetField.setText(myServerModel.getCharset());
    myJmxPortField.setText(String.valueOf(myServerModel.getJmxPort()));

//    myTransportManagerConfigurable.setHostId(myServerModel.getTransportHostId(), myServerModel.getProject());
    myDeploymentTransportTarget = getOrCreateTransportTarget(myServerModel.getTransportTargetWebApps());
//    myDeploymentTransportTargetConfigurable.setTarget(myDeploymentTransportTarget);

    updateJmxPortVisible(myServerModel);
  }

  @Override
  protected void applyEditorTo(@NotNull CommonModel s) throws ConfigurationException {
    final ResinRemoteModel serverModel = ((ResinRemoteModel)s.getServerModel());

    serverModel.setJmxPort(parseJmxPort());
    serverModel.setCharset(myCharsetField.getText());

//    serverModel.setTransportHostId(myTransportManagerConfigurable.getHostId());
    serverModel.setTransportTargetWebApps(myDeploymentTransportTarget);
//    myDeploymentTransportTargetConfigurable.saveState();
  }

  // TODO: copy-pasted from DM
  private static TransportTarget getOrCreateTransportTarget(TransportTarget target) {
    return target == null || target.getId() == null ? TransportManager.createTarget() : target;
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return myRootPanel;
  }

  @Override
  protected void setJmxPortVisible(boolean visible) {
    myJmxPortLabel.setVisible(visible);
    myJmxPortField.setVisible(visible);
    myPingButton.setVisible(visible);
  }
}
