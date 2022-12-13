package com.intellij.dmserver.install;

import com.intellij.dmserver.install.impl.DMServerInstallationImpl;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ComboboxWithBrowseButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DMServerInstallationEditor {
  @Nullable
  private final Project myProject;
  private final ComboboxWithBrowseButton myFolderSelector;

  public DMServerInstallationEditor(final @Nullable Project project, boolean checkInstallation) {
    myProject = project;
    myFolderSelector = new ComboboxWithBrowseButton();
    myFolderSelector.addActionListener(new DMServerInstallationChecker(project, myFolderSelector, checkInstallation));
    final JComboBox comboBox = myFolderSelector.getComboBox();
    comboBox.setEditable(true);
    comboBox.removeAllItems();
    for (DMServerInstallation next : DMServerInstallationManager.getInstance().getValidInstallations()) {
      comboBox.addItem(FileUtil.toSystemDependentName(next.getHome().getPath()));
    }
  }

  public JComponent getJComponent() {
    return myFolderSelector;
  }

  public String getPath() {
    return FileUtil.toSystemIndependentName((String)myFolderSelector.getComboBox().getEditor().getItem());
  }

  public void setPath(final String path) {
    myFolderSelector.getComboBox().setSelectedItem(path != null ? FileUtil.toSystemDependentName(path) : null);
  }

  public void setDefaultPath() {
    final JComboBox comboBox = myFolderSelector.getComboBox();
    if (comboBox.getItemCount() > 0) {
      comboBox.setSelectedIndex(0);
    }
  }

  public JComboBox getComboBox() {
    return myFolderSelector.getComboBox();
  }


  private static class DMServerInstallationChecker extends ComponentWithBrowseButton.BrowseFolderActionListener<JComboBox> {
    private final ComboboxWithBrowseButton myPathEditor;
    private final boolean myCheckInstallation;

    DMServerInstallationChecker(@Nullable Project project, final ComboboxWithBrowseButton pathEditor, boolean checkInstallation) {
      super(DmServerBundle.message("DMServerInstallationChecker.browse.name"),
            DmServerBundle.message("DMServerInstallationChecker.browse.description"),
            pathEditor, project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(), TextComponentAccessor.STRING_COMBOBOX_WHOLE_TEXT);
      myPathEditor = pathEditor;
      myCheckInstallation = checkInstallation;
    }

    @Override
    protected void onFileChosen(@NotNull VirtualFile chosenFile) {
      super.onFileChosen(chosenFile);
      if (myCheckInstallation) {
        final ValidationResult result = new DMServerInstallationImpl(chosenFile).validate();
        if (!result.isOk()) {
          Messages.showErrorDialog(myPathEditor, result.getErrorMessage());
        }
      }
    }
  }
}
