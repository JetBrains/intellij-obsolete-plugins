package com.intellij.dmserver.run.remote;

import com.intellij.dmserver.run.DMRunConfigurationEditorBase;
import com.intellij.dmserver.util.DmServerBundle;
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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DMRemoteRunConfigurationEditor extends DMRunConfigurationEditorBase {
  private JPanel myRootPanel;
  private JTextField myJmxPortField;
  private JTextField myJmxUserField;
  private JPasswordField myJmxPasswordField;
  private JLabel myJmxPasswordLabel;

  private JTextField myJmxMappingUrlField;
  private JButton myPingButton;
  private TransportManagerConfigurable myTransportManagerConfigurable;
  private TransportTargetConfigurable myDeploymentTransportTargetConfigurable;
  private TransportTargetConfigurable myRepositoryTransportTargetConfigurable;
  private JTextField myRepositoryTargetNameField;
  private JPanel myTransportManagerPanel;
  private JPanel myDeploymentTransportTargetPanel;
  private JPanel myRepositoryTransportTargetPanel;

  private TransportTarget myDeploymentTransportTarget;
  private TransportTarget myRepositoryTransportTarget;

  public DMRemoteRunConfigurationEditor(@NotNull Project project) {
    super(project);

    myPingButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onPingButton();
      }
    });

    getDeploymentTransportTargetConfigurable().setParentConfigurable(getTransportManagerConfigurable());
    getRepositoryTransportTargetConfigurable().setParentConfigurable(getTransportManagerConfigurable());
  }

  private TransportManagerConfigurable getTransportManagerConfigurable() {
    if (myTransportManagerConfigurable == null) {
      myTransportManagerConfigurable = new TransportManagerConfigurable();
    }
    return myTransportManagerConfigurable;
  }

  private TransportTargetConfigurable getDeploymentTransportTargetConfigurable() {
    if (myDeploymentTransportTargetConfigurable == null) {
      myDeploymentTransportTargetConfigurable = new TransportTargetConfigurable();
    }
    return myDeploymentTransportTargetConfigurable;
  }

  private TransportTargetConfigurable getRepositoryTransportTargetConfigurable() {
    if (myRepositoryTransportTargetConfigurable == null) {
      myRepositoryTransportTargetConfigurable = new TransportTargetConfigurable();
    }
    return myRepositoryTransportTargetConfigurable;
  }

  private void onPingButton() {
    final Ref<Boolean> success = new Ref<>(null);
    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      try {
        CommonModel commonModel = getSnapshot();
        DMServerRemoteModel serverModel = (DMServerRemoteModel)commonModel.getServerModel();
        success.set(serverModel.createServerInstance().connect());
      }
      catch (Exception ex) {
        success.set(false);
      }
    }, DmServerBundle.message("DMRemoteRunConfigurationEditor.message.ping.operation-name"), true, getProject())) {
      return;
    }

    if (success.isNull()) {
      return;
    }

    String ping = DmServerBundle.message("DMRemoteRunConfigurationEditor.message.ping");
    if (success.get()) {
      Messages.showInfoMessage(DmServerBundle.message("DMRemoteRunConfigurationEditor.message.ping.ok"), ping);
    }
    else {
      Messages.showErrorDialog(DmServerBundle.message("DMRemoteRunConfigurationEditor.message.ping.failed"), ping);
    }
  }

  @Override
  protected void resetEditorFrom(@NotNull CommonModel s) {
    DMServerRemoteModel serverModel = (DMServerRemoteModel)s.getServerModel();
    resetJmxConfiguration(serverModel);

    myJmxMappingUrlField.setText(notNull(serverModel.getJmxMappingUrl()));
    myRepositoryTargetNameField.setText(notNull(serverModel.getRepositoryName()));

    getTransportManagerConfigurable().setHostId(serverModel.getTransportHostId(), serverModel.getProject());
    myDeploymentTransportTarget = getOrCreateTransportTarget(serverModel.getTransportTargetJmx());
    getDeploymentTransportTargetConfigurable().setTarget(myDeploymentTransportTarget);
    myRepositoryTransportTarget = getOrCreateTransportTarget(serverModel.getTransportTargetRepository());
    getRepositoryTransportTargetConfigurable().setTarget(myRepositoryTransportTarget);
  }

  private void createUIComponents() {
    myTransportManagerPanel = getTransportManagerConfigurable().getMainPanel();
    myDeploymentTransportTargetPanel = getDeploymentTransportTargetConfigurable().getMainPanel();
    myRepositoryTransportTargetPanel = getRepositoryTransportTargetConfigurable().getMainPanel();
  }

  private static TransportTarget getOrCreateTransportTarget(TransportTarget target) {
    return target == null || target.getId() == null ? TransportManager.createTarget() : target;
  }

  @Override
  protected void applyEditorTo(@NotNull CommonModel s) throws ConfigurationException {
    final DMServerRemoteModel serverModel = (DMServerRemoteModel)s.getServerModel();
    applyJmxConfiguration(serverModel);

    serverModel.setJmxMappingUrl(myJmxMappingUrlField.getText());
    serverModel.setRepositoryName(myRepositoryTargetNameField.getText());

    serverModel.setTransportHostId(getTransportManagerConfigurable().getHostId());

    serverModel.setTransportTargetJmx(myDeploymentTransportTarget);
    serverModel.setTransportTargetRepository(myRepositoryTransportTarget);

    getDeploymentTransportTargetConfigurable().saveState();
    getRepositoryTransportTargetConfigurable().saveState();
  }

  @Override
  @NotNull
  protected JComponent createEditor() {
    return myRootPanel;
  }

  @Override
  protected JTextField getJmxPortField() {
    return myJmxPortField;
  }

  @Override
  protected JTextField getJmxUserField() {
    return myJmxUserField;
  }

  @Override
  protected JPasswordField getJmxPasswordField() {
    return myJmxPasswordField;
  }

  @Override
  protected JLabel getJmxPasswordLabel() {
    return myJmxPasswordLabel;
  }
}
