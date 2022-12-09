package com.intellij.dmserver.util;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

public final class UiUtil {
  public static void setupDirectoryPicker(TextFieldWithBrowseButton component,
                                          @Nls String title, @Nls String description,
                                          Project project,
                                          TextComponentAccessor<JTextField> textAccessor) {

    FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    component.addBrowseFolderListener(title, description, project, fileChooserDescriptor, textAccessor);
    installPathCompletion(component.getTextField(), fileChooserDescriptor);
  }

  private static void installPathCompletion(JTextField textField, FileChooserDescriptor fileChooserDescriptor) {
    final Application application = ApplicationManager.getApplication();
    if (application == null || application.isUnitTestMode() || application.isHeadlessEnvironment()) return;
    FileChooserFactory.getInstance().installFileCompletion(textField, fileChooserDescriptor, true, null);
  }
}
