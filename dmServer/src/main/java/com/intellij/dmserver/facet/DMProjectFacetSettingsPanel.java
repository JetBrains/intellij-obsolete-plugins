package com.intellij.dmserver.facet;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.install.DMServerInstallationManager;
import com.intellij.dmserver.integration.DMServerIntegration;
import com.intellij.dmserver.integration.DMServerIntegrationData;
import com.intellij.dmserver.osmorc.FrameworkUtils;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.appServers.integration.ApplicationServersUtil;
import com.intellij.javaee.appServers.serverInstances.ApplicationServersManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleListCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.osmorc.frameworkintegration.FrameworkInstanceDefinition;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class DMProjectFacetSettingsPanel {

  private JComboBox<ApplicationServer> myServerComboBox;
  private JButton myAddButton;
  private JPanel myMainPanel;

  private Project myProject;

  private boolean myInitialized = false;

  private DMServerInstallation mySelectedInstallation;
  private FrameworkInstanceDefinition mySelectedFramework;

  public DMProjectFacetSettingsPanel() {

  }

  public void init(final Project project) {
    if (myInitialized) {
      throw new RuntimeException("Should be called once");
    }
    myInitialized = true;

    myServerComboBox.setRenderer(SimpleListCellRenderer.create((label, value, index) -> {
      if (value != null) {
        label.setText(value.getName());
      }
      else {
        if (myServerComboBox.isEnabled()) {
          label.setText(DmServerBundle.message("DMProjectFacetSettingsPanel.error.html-message.none"));
        }
        else {
          label.setText(DmServerBundle.message("DMProjectFacetSettingsPanel.error.not-html.message.none"));
        }
      }
    }));

    myProject = project;
    mySelectedInstallation = project == null ? null : FrameworkUtils.getInstance().getActiveDMServerInstallation(project);

    updateSelectedFramework();
    ApplicationServer selectedServer = mySelectedInstallation == null || !mySelectedInstallation.isValid()
                                       ? null
                                       : mySelectedInstallation.getOrCreateApplicationServer();

    if (selectedServer == null) {
      addServer(null);
    }
    for (ApplicationServer server : DMServerIntegration.getInstance().getDMServers()) {
      addServer(server);
    }
    selectServer(selectedServer);

    myServerComboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          doSelectServer();
        }
      }
    });

    myAddButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doAddServer();
      }
    });
  }

  private void addServer(ApplicationServer server) {
    myServerComboBox.addItem(server);
  }

  private void selectServer(ApplicationServer server) {
    myServerComboBox.setSelectedItem(server);
  }

  public JPanel getMainPanel() {
    return myMainPanel;
  }

  private void doAddServer() {
    final ApplicationServersManager.ApplicationServersManagerModifiableModel model =
      ApplicationServersManager.getInstance().createModifiableModel();
    ApplicationServer server = ApplicationServersUtil
      .createNewApplicationServer(DMServerIntegration.getInstance(), model, DMServerIntegration.getInstance().getDMServers(), myMainPanel);
    if (server == null) {
      return;
    }
    addServer(server);
    WriteAction.run(() -> model.commit());
    selectServer(server);
  }

  private void doSelectServer() {
    updateSelectedInstallation();
    updateSelectedFramework();
    if (myProject != null) {
      applyFrameworkSelection(myProject);
    }
  }

  public void applyFrameworkSelection(@NotNull Project project) {
    FrameworkUtils.getInstance().setActiveFrameworkInstance(project, mySelectedFramework);
  }

  private void updateSelectedFramework() {
    mySelectedFramework =
      mySelectedInstallation == null ? null : DMServerInstallationManager.getInstance().findFramework(mySelectedInstallation);
  }

  public DMServerInstallation getSelectedServerInstallation() {
    return mySelectedInstallation;
  }

  private FrameworkInstanceDefinition getSelectedFramework() {
    return mySelectedFramework;
  }

  private void updateSelectedInstallation() {
    ApplicationServer server = (ApplicationServer)myServerComboBox.getSelectedItem();
    if (server != null && myServerComboBox.getItemAt(0) == null) {
      myServerComboBox.removeItem(null);
    }
    mySelectedInstallation = server == null
                             ? null
                             : DMServerInstallationManager.getInstance()
                               .findInstallation(((DMServerIntegrationData)server.getPersistentData()).getInstallationHome());
  }
}
