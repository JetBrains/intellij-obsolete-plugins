package org.intellij.j2ee.web.resin.ui;

import com.intellij.execution.ExecutionException;
import com.intellij.icons.AllIcons;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerPersistentDataEditor;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import org.intellij.j2ee.web.resin.ResinBundle;
import org.intellij.j2ee.web.resin.ResinPersistentData;
import org.intellij.j2ee.web.resin.resin.ResinInstallation;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import java.io.File;

public class SelectResinLocationEditor extends ApplicationServerPersistentDataEditor<ResinPersistentData> {

  @NonNls
  private static final String RESIN_CONF_FILE = "conf/resin.xml";
  @NonNls
  private static final String OLD_RESIN_CONF_FILE = "conf/resin.conf";

  private JPanel mainPanel;
  private TextFieldWithBrowseButton resinHomeSelector;
  private JLabel resinVersionLabel;
  private JCheckBox includeAllResinjarsCheckbox;
  private TextFieldWithBrowseButton defaultResinConf;
  private JLabel myErrorLabel;

  private boolean suggestConfPath = false;

  private boolean myHasHomeError;

  public SelectResinLocationEditor() {
    initChooser(resinHomeSelector, ResinBundle.message("message.text.locator.resin.home.title"),
                ResinBundle.message("message.text.locator.resin.home.summary"), false, true);
    initChooser(defaultResinConf, ResinBundle.message("message.text.locator.resin.conf.title"),
                ResinBundle.message("message.text.locator.resin.conf.summary"), true, false);

    resinHomeSelector.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void textChanged(@NotNull DocumentEvent event) {
        suggestConfPath = true;
        update();
      }
    });

    defaultResinConf.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void textChanged(@NotNull DocumentEvent event) {
        suggestConfPath = false;
        updateConfPath();
      }
    });

    includeAllResinjarsCheckbox.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        update();
      }
    });

    myErrorLabel.setIcon(AllIcons.General.BalloonError);
    update();
  }

  private void update() {
    final String homePath = resinHomeSelector.getText();

    myErrorLabel.setText("");

    ResinInstallation installation;
    try {
      installation = ResinInstallation.create(homePath);
    }
    catch (ExecutionException e) {
      myErrorLabel.setText(e.getMessage());
      myErrorLabel.setVisible(true);
      myHasHomeError = true;
      return;
    }

    hideError();
    myHasHomeError = false;

    if (!installation.isVersionDetected()) {
      resinVersionLabel.setText(ResinBundle.message("location.dlg.detected.version.unknown"));
    }

    else {
      resinVersionLabel.setText(installation.getVersion().toString());

      //Auto-select resin.conf on creating new app server instance
      if (suggestConfPath) {
        File resinConfDef = new File(homePath, FileUtil.toSystemDependentName(RESIN_CONF_FILE));
        if (resinConfDef.exists()) {
          defaultResinConf.setText(resinConfDef.getAbsoluteFile().getAbsolutePath());
        }
        else {
          resinConfDef = new File(homePath, FileUtil.toSystemDependentName(OLD_RESIN_CONF_FILE));
          if (resinConfDef.exists()) {
            defaultResinConf.setText(resinConfDef.getAbsoluteFile().getAbsolutePath());
          }
        }
      }
    }
    updateConfPath();
  }

  private void updateConfPath() {
    if (myHasHomeError) {
      return;
    }

    String confFilePath = defaultResinConf.getText();
    File confFile = StringUtil.isEmpty(confFilePath) ? null : new File(confFilePath);

    if (confFile == null) {
      showError(ResinBundle.message("message.error.resin.conf.doesnt.chosen"));
      return;
    }
    if (!confFile.exists()) {
      showError(ResinBundle.message("message.error.resin.conf.doesnt.exist", ""));
      return;
    }
    if (confFile.isDirectory()) {
      showError(ResinBundle.message("message.error.resin.conf.directory", confFile.getAbsolutePath()));
      return;
    }

    hideError();
  }

  private void showError(String errorMsg) {
    myErrorLabel.setText(errorMsg);
    myErrorLabel.setVisible(true);
  }

  private void hideError() {
    myErrorLabel.setVisible(false);
  }

  @Override
  protected void resetEditorFrom(@NotNull ResinPersistentData resinPersistentData) {
    this.suggestConfPath = resinPersistentData.RESIN_HOME == null;

    resinHomeSelector.setText(resinPersistentData.RESIN_HOME);
    includeAllResinjarsCheckbox.setSelected(resinPersistentData.INCLUDE_ALL_JARS);
    defaultResinConf.setText(resinPersistentData.RESIN_CONF);
    update();
  }

  @Override
  protected void applyEditorTo(@NotNull ResinPersistentData data) {
    data.RESIN_HOME = resinHomeSelector.getText();
    data.RESIN_CONF = defaultResinConf.getText();
    data.INCLUDE_ALL_JARS = includeAllResinjarsCheckbox.isSelected();
  }

  @Override
  @NotNull
  protected JComponent createEditor() {
    return mainPanel;
  }

  private static void initChooser(TextFieldWithBrowseButton field,
                                  String title,
                                  String description,
                                  boolean chooseFiles,
                                  boolean chooseDirs) {
    field.setText("");
    field.getTextField().setEditable(true);
    field.addBrowseFolderListener(title, description, null,
                                  new FileChooserDescriptor(chooseFiles, chooseDirs, false, false, false, false));
  }
}
