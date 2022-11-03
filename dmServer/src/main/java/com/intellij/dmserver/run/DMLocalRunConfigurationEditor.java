package com.intellij.dmserver.run;

import com.intellij.dmserver.integration.DMServerIntegrationData;
import com.intellij.dmserver.integration.DMServerRepositoryItem;
import com.intellij.dmserver.integration.DMServerRepositoryItemBase;
import com.intellij.dmserver.integration.DMServerRepositoryWatchedItem;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.appServers.run.configuration.ApplicationServerSelectionListener;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DMLocalRunConfigurationEditor extends DMRunConfigurationEditorBase implements ApplicationServerSelectionListener,
                                                                                           PanelWithAnchor {
  private JPanel myRootPanel;
  private JTextField myPortField;
  private JTextField myJmxPortField;
  private JTextField myJmxUserField;
  private JPasswordField myJmxPasswordField;
  private JComboBox<DMServerRepositoryWatchedItem> myRepositoryItemComboBox;
  private JBLabel myJMXPasswordLabel;
  private JComponent anchor;

  private final DMServerModel myModel;

  public DMLocalRunConfigurationEditor(Project project, DMServerModel model) {
    super(project);
    myModel = model;

    myRepositoryItemComboBox.setRenderer(SimpleListCellRenderer.create("", DMServerRepositoryItemBase::getPath));

    serverSelected(model.getCommonModel().getApplicationServer());

    setAnchor(myJMXPasswordLabel);
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
    return myJMXPasswordLabel;
  }

  @Override
  protected void resetEditorFrom(@NotNull CommonModel s) {
    DMServerModel serverModel = (DMServerModel)s.getServerModel();
    myPortField.setText(String.valueOf(serverModel.getLocalPort()));
    String repositoryFolder = serverModel.getTargetRepositoryFolder();
    if (repositoryFolder != null) {
      for (int i = 0; i < myRepositoryItemComboBox.getItemCount(); i++) {
        DMServerRepositoryWatchedItem repositoryItem = myRepositoryItemComboBox.getItemAt(i);
        if (repositoryFolder.equals(repositoryItem.getPath())) {
          myRepositoryItemComboBox.setSelectedItem(repositoryItem);
          break;
        }
      }
    }
    resetJmxConfiguration(serverModel);
  }

  @Override
  protected void applyEditorTo(@NotNull CommonModel s) throws ConfigurationException {
    final DMServerModel serverModel = (DMServerModel)s.getServerModel();
    serverModel.setPort(parseInt(myPortField, "DMLocalRunConfigurationEditor.bad.port"));
    DMServerRepositoryWatchedItem selectedRepositoryItem = (DMServerRepositoryWatchedItem)myRepositoryItemComboBox.getSelectedItem();
    serverModel.setTargetRepositoryFolder(selectedRepositoryItem == null ? null : selectedRepositoryItem.getPath());
    applyJmxConfiguration(serverModel);
  }

  @Override
  public void serverSelected(@Nullable ApplicationServer server) {
    myRepositoryItemComboBox.removeAllItems();
    if (server != null) {
      DMServerIntegrationData integrationData = (DMServerIntegrationData)server.getPersistentData();
      for (DMServerRepositoryItem repositoryItem : integrationData.getRepositoryItems()) {
        if (repositoryItem instanceof DMServerRepositoryWatchedItem) {
          myRepositoryItemComboBox.addItem((DMServerRepositoryWatchedItem)repositoryItem);
        }
      }
    }
  }

  @Override
  public void serverProbablyEdited(@Nullable ApplicationServer server) {

  }

  @Override
  @NotNull
  protected JComponent createEditor() {
    return myRootPanel;
  }

  @Override
  public JComponent getAnchor() {
    return anchor;
  }

  @Override
  public void setAnchor(JComponent anchor) {
    this.anchor = anchor;
    myJMXPasswordLabel.setAnchor(anchor);
  }
}
