package com.jetbrains.plugins.compass;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.*;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Alarm;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.sass.extensions.SassRubyIntegrationHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.notNullize;
import static com.intellij.openapi.vfs.VfsUtilCore.pathToUrl;
import static com.intellij.xml.util.XmlStringUtil.wrapInHtml;

public class CompassSettingsPanelImpl implements CompassSettingsPanel {
  private static final Logger LOGGER = Logger.getInstance(CompassSettingsPanelImpl.class);
  @NotNull private final Module myModule;
  @NotNull private final Alarm myValidationAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
  @NotNull private final List<String> myExecutableFilesVariants;
  @NotNull private final List<String> myConfigFilesVariants;
  private JBCheckBox myCompassEnabledCheckBox;
  private TextFieldWithHistoryWithBrowseButton myCompassExecutableFileTextField;
  private TextFieldWithHistoryWithBrowseButton myCompassConfigPathTextField;
  private JPanel myPanel;
  private JBLabel myErrorLabel;
  private JPanel mySettingsPanel;
  private String myLastValidatedExecutablePath;

  public CompassSettingsPanelImpl(@NotNull Module module,
                                  @NotNull final List<String> executableFilesVariants,
                                  @NotNull final List<String> configFilesVariants, boolean fullMode) {
    myCompassExecutableFileTextField.setPreferredSize(new Dimension(fullMode ? 100 : 400, -1));
    myCompassConfigPathTextField.setPreferredSize(new Dimension(fullMode ? 100 : 400, -1));
    myModule = module;
    myExecutableFilesVariants = executableFilesVariants;
    myConfigFilesVariants = configFilesVariants;
    myErrorLabel.setVisible(false);
    myCompassEnabledCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        updateUiComponents();
      }
    });
    final Project project = module.getProject();
    initCompassExecutableFileTextField(project, executableFilesVariants);
    initCompassConfigPathTextField(project, configFilesVariants, myCompassConfigPathTextField);
  }

  public static void initCompassConfigPathTextField(@NotNull Project project, @NotNull List<String> configFilesVariants, TextFieldWithHistoryWithBrowseButton textField) {
    final FileChooserDescriptor configFileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
    configFileChooserDescriptor.setRoots(ProjectRootManager.getInstance(project).getContentRoots());
    configFileChooserDescriptor.setShowFileSystemRoots(true);
    configFileChooserDescriptor.withTreeRootVisible(true);
    textField.addBrowseFolderListener("Select compass config file", null, project,
                                      configFileChooserDescriptor,
                                      TextComponentAccessor.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT);
    TextFieldWithHistory childComponent = textField.getChildComponent();
    //reinstall completion in order to complete hidden files
    FileChooserFactory.getInstance().installFileCompletion(childComponent.getTextEditor(), configFileChooserDescriptor, true, project);
    childComponent.setHistory(configFilesVariants);
    childComponent.setMinimumAndPreferredWidth(childComponent.getWidth());
  }

  @Override
  public boolean isModified(@Nullable CompassSettings compassSettings) {
    return isEnableOptionChanged(compassSettings) || isConfigFilePathChanged(compassSettings) || isExecutablePathChanged(compassSettings);
  }

  @Override
  public boolean isConfigFilePathChanged(@Nullable CompassSettings compassSettings) {
    return compassSettings != null && !compassSettings.getCompassConfigPath().equals(getCompassConfigPath());
  }

  @Override
  public boolean isExecutablePathChanged(@Nullable CompassSettings compassSettings) {
    return compassSettings != null && !compassSettings.getCompassExecutableFilePath().equals(getCompassExecutableFilePath());
  }

  @Override
  public boolean isEnableOptionChanged(@Nullable CompassSettings compassSettings) {
    return compassSettings != null && compassSettings.isCompassSupportEnabled() != isCompassSupportEnabled();
  }

  @Override
  public void apply(@Nullable CompassSettings compassSettings) {
    if (compassSettings != null) {
      compassSettings.setCompassSupportEnabled(isCompassSupportEnabled());
      compassSettings.setCompassExecutableFilePath(getCompassExecutableFilePath());
      compassSettings.setCompassConfigPath(getCompassConfigPath());
    }
  }

  @Override
  public void reset(@Nullable final CompassSettings compassSettings) {
    if (compassSettings != null) {
      final boolean compassSupportEnabled = compassSettings.isCompassSupportEnabled();
      myCompassEnabledCheckBox.setSelected(compassSupportEnabled);
      myCompassExecutableFileTextField.getChildComponent().setText(compassSettings.getCompassExecutableFilePath());
      myCompassConfigPathTextField.getChildComponent().setText(compassSettings.getCompassConfigPath());
    }

    if (compassSettings == null) {
      UIUtil.setEnabled(myPanel, false, true);
    }
    updateUiComponents();
  }

  private boolean isCompassSupportEnabled() {
    return myCompassEnabledCheckBox.isSelected();
  }

  @NotNull
  private String getCompassExecutableFilePath() {
    return notNullize(myCompassExecutableFileTextField.getChildComponent().getText());
  }

  @NotNull
  private String getCompassConfigPath() {
    return notNullize(myCompassConfigPathTextField.getChildComponent().getText());
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  private void updateUiComponents() {
    UIUtil.setEnabled(mySettingsPanel, myCompassEnabledCheckBox.isSelected(), true);

    if (myCompassEnabledCheckBox.isSelected()) {
      if (myCompassExecutableFileTextField.getChildComponent().getText().isEmpty()) {
        myCompassExecutableFileTextField.getChildComponent().setText(ContainerUtil.getFirstItem(myExecutableFilesVariants, ""));
      }

      if (myCompassConfigPathTextField.getChildComponent().getText().isEmpty()) {
        myCompassConfigPathTextField.getChildComponent().setText(ContainerUtil.getFirstItem(myConfigFilesVariants, ""));
      }
    }
    myLastValidatedExecutablePath = null;
    scheduleExecutablePathValidation();
  }

  @Nullable
  private ValidationInfo validateExecutablePath() {
    if (!myCompassEnabledCheckBox.isSelected()) {
      return null;
    }
    final String currentExecutablePath = myCompassExecutableFileTextField.getChildComponent().getText();
    if (myLastValidatedExecutablePath == null || !myLastValidatedExecutablePath.equals(currentExecutablePath)) {
      myLastValidatedExecutablePath = currentExecutablePath;
      if (StringUtil.isEmpty(currentExecutablePath)) {
        return new ValidationInfo("Cannot find compass executable file");
      }
      final VirtualFile compassFile = VirtualFileManager.getInstance().refreshAndFindFileByUrl(pathToUrl(currentExecutablePath));
      if (compassFile == null || compassFile.isDirectory() || compassFile.is(VFileProperty.SPECIAL)) {
        return new ValidationInfo("Cannot find compass executable file");
      }
      else {
        try {
          ProcessOutput output = SassRubyIntegrationHelper.getInstance().execScript(myModule, null, currentExecutablePath);
          if (output.getExitCode() != 0) {
            LOGGER.info("Cannot launch compass executable file" + "\n" +
                        "code: " + output.getExitCode() + "\n" +
                        "stderr: " + output.getStderr() + "\n" +
                        "stdout: " + output.getStdout());
            return new ValidationInfo("Cannot launch compass executable file");
          }
        }
        catch (ExecutionException e) {
          LOGGER.info(e);
          return new ValidationInfo("Cannot launch compass executable file");
        }
      }
    }
    return null;
  }

  private void initCompassExecutableFileTextField(@NotNull Project project, @NotNull List<String> executableFilesVariants) {
    final FileChooserDescriptor compassFileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
    myCompassExecutableFileTextField.addBrowseFolderListener("Select compass executable file",
                                                             null, project, compassFileChooserDescriptor,
                                                             TextComponentAccessor.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT);
    TextFieldWithHistory childComponent = myCompassExecutableFileTextField.getChildComponent();
    //reinstall completion in order to complete hidden files
    FileChooserFactory.getInstance().installFileCompletion(childComponent.getTextEditor(), compassFileChooserDescriptor, true, project);

    childComponent.setHistory(executableFilesVariants);
    childComponent.setMinimumAndPreferredWidth(childComponent.getWidth());

    childComponent.getTextEditor().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        scheduleExecutablePathValidation();
      }
    });
  }

  private void scheduleExecutablePathValidation() {
    myErrorLabel.setVisible(false);
    myValidationAlarm.cancelAllRequests();
    if (!myValidationAlarm.isDisposed()) {
      myValidationAlarm.addRequest(() -> {
        final ValidationInfo info = validateExecutablePath();
        ApplicationManager.getApplication().invokeLater(() -> {
          if (info != null) {
            myErrorLabel.setText(wrapInHtml("<font color='#" + ColorUtil.toHex(JBColor.RED) + "'><left>" + info.message + "</left></font>"));
            myErrorLabel.setIcon(AllIcons.Actions.Lightning);
            myErrorLabel.setVisible(true);
          }
          else {
            myErrorLabel.setVisible(false);
          }
          myErrorLabel.revalidate();
        });
      }, 700);
    }
  }

  @Override
  public void dispose() {
    myValidationAlarm.cancelAllRequests();
  }
}
