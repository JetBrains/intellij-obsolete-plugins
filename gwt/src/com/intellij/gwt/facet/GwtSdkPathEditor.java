package com.intellij.gwt.facet;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.ui.ComboboxWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JTextField;

public class GwtSdkPathEditor {
  private final ComboboxWithBrowseButton myPathEditor;

  public GwtSdkPathEditor(@Nullable Project project) {
    myPathEditor = new ComboboxWithBrowseButton();
    var descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
      .withTitle(GwtBundle.message("gwt.installation.chooser.title"))
      .withDescription(GwtBundle.message("gwt.installation.chooser.description"));
    myPathEditor.addBrowseFolderListener(project, descriptor, TextComponentAccessor.STRING_COMBOBOX_WHOLE_TEXT);
    JComboBox comboBox = myPathEditor.getComboBox();
    comboBox.setEditable(true);
    GwtSdkManager.getInstance().removeInvalidSdk();
    for (@NlsSafe String path : GwtSdkManager.getInstance().getAllSdkPaths()) {
      comboBox.addItem(path);
    }
  }

  public ComboboxWithBrowseButton getMainComponent() {
    return myPathEditor;
  }

  public JComboBox getComboBox() {
    return myPathEditor.getComboBox();
  }

  public String getPath() {
    return (String)myPathEditor.getComboBox().getEditor().getItem();
  }

  public @Nullable GwtSdk getSelectedSdk() {
    if (StringUtil.isEmpty(getPath())) {
      return null;
    }
    return GwtSdkManager.getInstance().getGwtSdk(getUrl());
  }

  public void setPath(@NlsSafe String path) {
    myPathEditor.getComboBox().setSelectedItem(path);
  }

  public String getUrl() {
    return VfsUtilCore.pathToUrl(FileUtil.toSystemIndependentName(getPath()));
  }

  public JTextField getPathTextField() {
    return (JTextField)getComboBox().getEditor().getEditorComponent();
  }
}
