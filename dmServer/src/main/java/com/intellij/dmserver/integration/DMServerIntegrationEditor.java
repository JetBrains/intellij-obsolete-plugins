package com.intellij.dmserver.integration;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.install.DMServerInstallationManager;
import com.intellij.dmserver.install.ServerVersionHandler;
import com.intellij.dmserver.install.impl.DMServerInstallationImpl;
import com.intellij.dmserver.libraries.LibrariesDialog;
import com.intellij.dmserver.libraries.LibrariesDialogCreator;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.dmserver.util.IconUtils;
import com.intellij.dmserver.util.UiUtil;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerPersistentDataEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.osmorc.frameworkintegration.FrameworkInstanceDefinition;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DMServerIntegrationEditor extends ApplicationServerPersistentDataEditor<DMServerIntegrationData> {

  private TextFieldWithBrowseButton myInstallationHomeField;
  private JPanel myMainPanel;
  private TextFieldWithBrowseButton myPickupDirectoryField;
  private JSpinner myDeployerTimeoutField;
  private JCheckBox myShellEnabledCheckBox;
  private JCheckBox myLogSystemOutCheckBox;
  private JCheckBox myLogSystemErrCheckBox;
  private TextFieldWithBrowseButton myServerLogsRootFolder;
  private JSpinner myShellPortField;
  private JButton myExportButton;
  private JButton myImportButton;
  private JButton myEditFrameworkButton;
  private JTextField myFrameworkNameField;
  private JPanel myStatusPanel;
  private JBLabel myStatusErrorLabel;
  private JBLabel myStatusValidLabel;
  private final DMServerRepositoryEditor myRepositoryEditor;
  private JLabel myReadErrorLabel;
  private JPanel myRepositoryPanel;

  private boolean myFieldsEnabled;
  private boolean myLockRefreshHome = false;

  private VirtualFile myHome;
  private DMServerInstallation myInstallation;
  private FrameworkInstanceDefinition myFramework;
  private Project myProject;

  private final PathResolver myPathResolver;

  public DMServerIntegrationEditor() {
    myPathResolver = new PathResolver() {

      @Override
      protected VirtualFile getBaseDir() {
        return myHome;
      }
    };
    myRepositoryEditor = new DMServerRepositoryEditor();
    myRepositoryEditor.setParent(this);
    myRepositoryPanel.add(myRepositoryEditor.createComponent(), BorderLayout.CENTER);

    setupDirectoryPicker(myInstallationHomeField, DmServerBundle.message("DMServerIntegrationEditor.label.setup.server.home"), false);
    myInstallationHomeField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        if (myLockRefreshHome) {
          return;
        }
        refreshHome(true);
      }
    });
    setupSpinner(myDeployerTimeoutField);
    setupSpinner(myShellPortField);
    setupDirectoryPicker(myPickupDirectoryField, DmServerBundle.message("DMServerIntegrationEditor.label.setup.pickup.folder"), true);
    setupDirectoryPicker(myServerLogsRootFolder, DmServerBundle.message("DMServerIntegrationEditor.label.setup.logs.folder"), true);
    myShellEnabledCheckBox.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        refreshShellPortEnabled();
      }
    });
    myImportButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doImport();
      }
    });
    myExportButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doExport();
      }
    });
    myEditFrameworkButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doManageBundles();
      }
    });

    IconUtils.setupWarningLabel(myStatusErrorLabel);

    IconUtils.setupWarningLabel(myReadErrorLabel);
    setReadError(false);
    myRepositoryEditor.setListener(new DMServerRepositoryEditorListener() {

      @Override
      public void itemAdded() {
        setReadError(false);
      }
    });

    refreshUi();
  }

  private void setReadError(boolean hasReadError) {
    myReadErrorLabel.setVisible(hasReadError);
  }

  private boolean hasReadError() {
    return myReadErrorLabel.isVisible();
  }

  // may refactor out

  private void refreshStatus(boolean valid) {
    @NonNls String cardName = valid ? "valid" : "error";

    ((CardLayout)myStatusPanel.getLayout()).show(myStatusPanel, cardName);
    if (valid) {
      myStatusValidLabel.setText(
        DmServerBundle.message("DMServerIntegrationEditor.label.detected.version", myInstallation.getVersionName()));
    }
  }

  private static void setupSpinner(JSpinner spinner) {
    spinner.setModel(new SpinnerNumberModel(0, 0, null, 1));
    spinner.setEditor(new JSpinner.NumberEditor(spinner, "0"));
  }

  private void doManageBundles() {
    LibrariesDialog dialog = LibrariesDialogCreator.createDialog(myFramework, myProject, myInstallation);
    dialog.show();
  }

  private void refreshUi() {
    refreshHome(false);
    refreshShellPortEnabled();
  }

  private void refreshHome(boolean importIfValid) {
    myHome = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(myInstallationHomeField.getText()));
    myInstallation = myHome == null ? null : new DMServerInstallationImpl(myHome);
    boolean installationValid = isInstallationValid();
    setServerFieldsEnabled(installationValid,
                           installationValid && myInstallation.getServerVersion().getVersion() != ServerVersionHandler.DMVersion.DM_10);
    refreshFramework(installationValid);
    refreshStatus(installationValid);
    if (importIfValid && installationValid) {
      doImport();
    }
  }

  private boolean isInstallationValid() {
    return myInstallation != null && myInstallation.isValid();
  }

  private void refreshFramework(boolean installationValid) {
    myFramework = null;
    myProject = null;
    if (installationValid) {
      myFramework = DMServerInstallationManager.getInstance().findFramework(myInstallation, false);
    }
    boolean frameworkExists = myFramework != null;
    myFrameworkNameField.setText(installationValid ? (frameworkExists
                                                      ? myFramework.getName()
                                                      : DmServerBundle
                                                        .message("DMServerIntegrationEditor.message.framework-not-yet-created")) : "");
    if (frameworkExists) {
      Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
      if (openProjects.length > 0) {
        myProject = openProjects[0];
      }
    }
    myEditFrameworkButton.setEnabled(myProject != null);
  }

  private void setServerFieldsEnabled(boolean enabled, boolean logsEnabled) {
    myFieldsEnabled = enabled;
    myPickupDirectoryField.setEnabled(enabled);
    myDeployerTimeoutField.setEnabled(enabled);
    myShellEnabledCheckBox.setEnabled(enabled);
    myLogSystemOutCheckBox.setEnabled(logsEnabled);
    myLogSystemErrCheckBox.setEnabled(logsEnabled);
    if (!logsEnabled) {
      myLogSystemOutCheckBox.setSelected(true);
      myLogSystemErrCheckBox.setSelected(true);
    }
    myServerLogsRootFolder.setEnabled(enabled);
    myShellPortField.setEnabled(enabled);
    myExportButton.setEnabled(enabled);
    myImportButton.setEnabled(enabled);
    myEditFrameworkButton.setEnabled(enabled);
    myRepositoryEditor.setEnabled(enabled);
  }

  private void setupDirectoryPicker(TextFieldWithBrowseButton field, @Nls String description, boolean allowRelative) {
    UiUtil.setupDirectoryPicker(field,
                                DmServerBundle.message("DMServerIntegrationEditor.browse.title"), description,
                                null,
                                allowRelative //
                                ? new TextComponentAccessor<>() {
                                  @Override
                                  public String getText(JTextField component) {
                                    return getPathResolver().path2Absolute(component.getText());
                                  }

                                  @Override
                                  public void setText(JTextField component, @NotNull String text) {
                                    component.setText(getPathResolver().path2Relative(text));
                                  }
                                } //
                                : TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
  }

  @Override
  protected void resetEditorFrom(@NotNull DMServerIntegrationData data) {
    myLockRefreshHome = true;
    try {
      myInstallationHomeField.setText(FileUtil.toSystemDependentName(data.getInstallationHome()));
    }
    finally {
      myLockRefreshHome = false;
    }
    loadFieldsFrom(data);
  }

  private void loadFieldsFrom(DMServerIntegrationData data) {
    refreshUi();
    myPickupDirectoryField.setText(FileUtil.toSystemDependentName(data.getPickupFolder()));
    myDeployerTimeoutField.setValue(data.getDeploymentTimeoutSecs());
    myShellEnabledCheckBox.setSelected(data.isShellEnabled());
    myLogSystemOutCheckBox.setSelected(data.isWrapSystemOut());
    myLogSystemErrCheckBox.setSelected(data.isWrapSystemErr());
    myServerLogsRootFolder.setText(FileUtil.toSystemDependentName(data.getDumpsFolder()));
    myShellPortField.setValue(data.getShellPort());
    myRepositoryEditor.loadFrom(data, myInstallation == null ? null : myInstallation.getServerVersion().getVersion());
    setReadError(data.isReloadRequired());
  }

  @Override
  protected void applyEditorTo(@NotNull DMServerIntegrationData data) {
    doApplyEditorTo(data);
  }

  private void doApplyEditorTo(DMServerIntegrationData data) {
    data.setInstallationHome(FileUtil.toSystemIndependentName(myInstallationHomeField.getText()));
    data.setPickupFolder(FileUtil.toSystemIndependentName(myPickupDirectoryField.getText()));
    data.setDeploymentTimeoutSecs(((Integer)myDeployerTimeoutField.getValue()).intValue());
    data.setShellEnabled(myShellEnabledCheckBox.isSelected());
    data.setWrapSystemOut(myLogSystemOutCheckBox.isSelected());
    data.setWrapSystemErr(myLogSystemErrCheckBox.isSelected());
    data.setDumpsFolder(FileUtil.toSystemIndependentName(myServerLogsRootFolder.getText()));
    data.setShellPort(((Integer)myShellPortField.getValue()).intValue());
    data.setReloadRequired(hasReadError());
    myRepositoryEditor.applyTo(data);
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return myMainPanel;
  }

  private void refreshShellPortEnabled() {
    boolean shellEnabled = myShellEnabledCheckBox.isSelected();
    myShellPortField.setEnabled(myFieldsEnabled && shellEnabled);
  }

  private void doExport() {
    DMServerIntegrationData data = new DMServerIntegrationData(myInstallationHomeField.getText());

    doApplyEditorTo(data);

    myInstallation.getConfigSupport().writeToServer(data);
  }

  private void doImport() {
    DMServerIntegrationData data = new DMServerIntegrationData(myInstallationHomeField.getText());

    myInstallation.getConfigSupport().readFromServer(data);

    data.setReloadRequired(false);

    loadFieldsFrom(data);
  }

  public PathResolver getPathResolver() {
    return myPathResolver;
  }
}

