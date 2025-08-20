package com.intellij.tcserver.server.integration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.tcserver.sdk.TcServerUtil;
import com.intellij.tcserver.util.TcServerBundle;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.io.File;
import java.util.List;

public class ServerInstanceCreatorDialog extends DialogWrapper {
  private JPanel myMainPanel;
  private JTextField myServerNameField;
  private JLabel myErrorLabel;
  private TextFieldWithBrowseButton myTemplatePathField;
  private JPanel myBefore2_1TemplatePanel;
  private JPanel myAfter2_1TemplatePanel;
  private JComboBox myTemplateComboBox;
  private final TcServerEditor myServerEditor;
  @Nls private String myTemplatePathErrorMessage;
  @Nls private String myServerNameErrorMessage;
  //validness doesn`t mean there is no message
  private boolean isTemplatePathValid = true;
  private boolean isServerNameValid = true;

  public ServerInstanceCreatorDialog(TcServerEditor serverEditor) throws RuntimeConfigurationException {
    super(serverEditor.getComponent(), false);
    myServerEditor = serverEditor;

    setDefaultTemplatePath();

    setTitle(TcServerBundle.message("serverInstanceCreatorDialog.title"));

    myErrorLabel.setIcon(AllIcons.General.BalloonError);
    setModal(true);
    init();

    myServerNameField.getDocument().addDocumentListener(new ServerNameValidationDocumentListener());
    myServerNameField.setText(getDefaultInstanceName());
    myServerNameField.setSelectionStart(0);
    myServerNameField.setSelectionEnd(myServerNameField.getText().length());

    myErrorLabel.setVisible(false);

    TcServerVersion version = serverEditor.getVersion();
    if (version == TcServerVersion.BEFORE_2_1) {
      myTemplatePathField.getChildComponent().getDocument().addDocumentListener(new TemplatePathValidationListener());
      myAfter2_1TemplatePanel.setVisible(false);
    }
    else if (version == TcServerVersion.EQUAL_OR_AFTER_2_1) {
      myBefore2_1TemplatePanel.setVisible(false);
      DefaultComboBoxModel model = (DefaultComboBoxModel)myTemplateComboBox.getModel();
      model.removeAllElements();
      try {
        List<String> templates = version.listTemplates(myServerEditor.getSdkPath());
        for (@NlsSafe String templateName : templates) {
          model.addElement(templateName);
        }
      }
      catch (ExecutionException e) {
        myErrorLabel.setText(e.getMessage());
        myErrorLabel.setVisible(true);
      }
    }
    else {
      throw new RuntimeConfigurationException(TcServerBundle.message("serverEditor.reloadInstances"));
    }
  }

  public String getInstanceName() {
    return myServerNameField.getText();
  }

  public String getTemplatePath(TcServerVersion version) throws RuntimeConfigurationException {
    if (version == TcServerVersion.BEFORE_2_1) {
      return myTemplatePathField.getText();
    }
    else if (version == TcServerVersion.EQUAL_OR_AFTER_2_1) {
      return (String)myTemplateComboBox.getModel().getSelectedItem();
    }
    throw new RuntimeConfigurationException(TcServerBundle.message("serverEditor.unknownVersion"));
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myServerNameField;
  }

  private void setDefaultTemplatePath() {
    String path = myServerEditor.getSdkPath();
    File templateDir = new File(TcServerUtil.getTemplatesPath(path));
    String defaultText;
    if (templateDir.exists() && templateDir.isDirectory()) {
      defaultText = templateDir.getPath();
    }
    else {
      defaultText = path;
    }
    final String defaultPath = defaultText;

    myTemplatePathField.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>(
      TcServerBundle.message("additionalDeploymentSettings.editorTitle"),//
      TcServerBundle.message("additionalDeploymentSettings.editorDescription"),//
      myTemplatePathField, null, FileChooserDescriptorFactory.createSingleFolderDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT) {
      @Override
      protected VirtualFile getInitialFile() {
        String directoryName = getComponentText();
        if (directoryName.length() == 0) {
          return LocalFileSystem.getInstance().findFileByPath(defaultPath);
        }
        else {
          return super.getInitialFile();
        }
      }
    });
  }

  private String getDefaultInstanceName() {
    String serverName = TcServerBundle.message("serverInstanceCreatorDialog.defaultInstanceName");
    if (myServerEditor.containsServerName(serverName)) {
      boolean found = false;
      int i = 1;
      while (!found) {
        String newName = serverName + i;
        if (!myServerEditor.containsServerName(newName)) {
          serverName = newName;
          found = true;
        }
        i++;
      }
    }
    return serverName;
  }

  private void notifyValidationResults() {
    if (myTemplatePathErrorMessage != null || myServerNameErrorMessage != null) {
      String text = myTemplatePathErrorMessage != null ? myTemplatePathErrorMessage : myServerNameErrorMessage;
      myErrorLabel.setText(text);
      myErrorLabel.setVisible(true);
    }
    else {
      myErrorLabel.setVisible(false);
    }

    //pack();
    setOKActionEnabled(isServerNameValid && isTemplatePathValid);
  }

  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }

  private class TemplatePathValidationListener extends DocumentAdapter {

    @Override
    protected void textChanged(@NotNull DocumentEvent event) {
      String path = myTemplatePathField.getText();
      myTemplatePathErrorMessage = null;
      isTemplatePathValid = true;

      try {
        TcServerUtil.validatePath(path);
        if (!StringUtil.isEmpty(path) && !new File(path).exists()) {
          myTemplatePathErrorMessage = TcServerBundle.message("serverInstanceCreatorDialog.fileNotFound", path);
          isTemplatePathValid = false;
        }
      }
      catch (RuntimeConfigurationException e) {
        myTemplatePathErrorMessage = TcServerBundle.message("serverInstanceCreatorDialog.invalidTemplatePath", e.getMessage());
        isTemplatePathValid = false;
      }

      notifyValidationResults();
    }
  }

  private class ServerNameValidationDocumentListener extends DocumentAdapter {
    @Override
    protected void textChanged(@NotNull DocumentEvent event) {
      String name = myServerNameField.getText();
      myServerNameErrorMessage = null;
      isServerNameValid = true;
      if (StringUtil.isEmpty(name)) {
        myServerNameErrorMessage = TcServerBundle.message("serverInstanceCreatorDialog.emptyServerName");
        isServerNameValid = false;
        notifyValidationResults();
        return;
      }

      try {
        TcServerUtil.validateServerName(name);
      }
      catch (RuntimeConfigurationException e) {
        myServerNameErrorMessage = e.getMessage();
        isServerNameValid = false;
        notifyValidationResults();
        return;
      }

      if (myServerEditor.containsServerName(name)) {
        myServerNameErrorMessage = TcServerBundle.message("serverInstanceCreatorDialog.instanceWillBeOverridden", name);
      }
      notifyValidationResults();
    }
  }
}
