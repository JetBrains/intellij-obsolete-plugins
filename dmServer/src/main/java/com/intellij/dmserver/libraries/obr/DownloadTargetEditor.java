package com.intellij.dmserver.libraries.obr;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.integration.RepositoryPattern;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleListCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class DownloadTargetEditor {

  private JComboBox<RepositoryPattern> myRepositoryItemComboBox;
  private TextFieldWithBrowseButton mySubFolderField;
  private JPanel myMainPanel;

  private final FileChooserDescriptor myFileChooserDescriptor;
  private boolean myChooseSubFolder;
  private VirtualFile myBaseDir;

  public DownloadTargetEditor() {
    myRepositoryItemComboBox.setRenderer(SimpleListCellRenderer.create("", value -> {
      @NlsSafe String path = value.getSource().getPath();
      return path;
    }));
    myRepositoryItemComboBox.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          doSelectRepositoryItem();
        }
      }
    });
    myFileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    myFileChooserDescriptor.withTreeRootVisible(true);
    mySubFolderField.addBrowseFolderListener(DmServerBundle.message("DownloadTargetEditor.browse.title"),
                                             DmServerBundle.message("DownloadTargetEditor.browse.description"), null,
                                             myFileChooserDescriptor,
                                             new TextComponentAccessor<>() {

                                               @Override
                                               public String getText(JTextField component) {
                                                 return getBaseDir().getPath() + "/" + component.getText();
                                               }

                                               @Override
                                               public void setText(JTextField component, @NotNull String text) {
                                                 component.setText(FileUtil.getRelativePath(
                                                   getBaseDir().getPath(),
                                                   FileUtil.toSystemIndependentName(text), '/'));
                                               }
                                             });
  }

  private VirtualFile getBaseDir() {
    return myBaseDir;
  }

  @Nullable
  public VirtualFile getTargetDir() {
    VirtualFile result = getBaseDir();
    if (myChooseSubFolder) {
      result = result.findFileByRelativePath(mySubFolderField.getText());
    }
    return result;
  }

  public void init(DMServerInstallation installation) {
    myRepositoryItemComboBox.removeAllItems();
    for (RepositoryPattern repositoryPattern : installation.collectRepositoryPatterns()) {
      myRepositoryItemComboBox.addItem(repositoryPattern);
    }
    doSelectRepositoryItem();
  }

  private void doSelectRepositoryItem() {
    RepositoryPattern repositoryPattern = getSelectedRepositoryPattern();
    boolean hasRepositoryPattern = repositoryPattern != null;
    myBaseDir = hasRepositoryPattern ? repositoryPattern.findBaseDir() : null;
    myChooseSubFolder = myBaseDir != null && repositoryPattern.hasDirPatterns();
    mySubFolderField.setEnabled(myChooseSubFolder);
    mySubFolderField.setText("");
    if (myChooseSubFolder) {
      myFileChooserDescriptor.setRoots(myBaseDir);
    }
  }

  private RepositoryPattern getSelectedRepositoryPattern() {
    return (RepositoryPattern)myRepositoryItemComboBox.getSelectedItem();
  }
}
