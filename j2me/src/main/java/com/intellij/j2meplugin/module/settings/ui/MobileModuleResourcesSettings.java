/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings.ui;

import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.module.J2MEModuleProperties;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;


public class MobileModuleResourcesSettings implements ModuleConfigurationEditor {
  private static final Logger LOG = Logger.getInstance("#com.intellij.j2meplugin");
  private final Module myModule;
  private final J2MEModuleProperties myProperties;
  private final ModifiableRootModel myRootModel;

  public MobileModuleResourcesSettings(Module module, ModifiableRootModel rootModel) {
    myRootModel = rootModel;
    myModule = module;
    myProperties = J2MEModuleProperties.getInstance(myModule);
    myDefaultResourcesDir = ReadAction.compute(() -> {
      @NonNls final String res = "res";
      return new File(myModule.getModuleFilePath()).getParent().replace('/', File.separatorChar) + File.separatorChar + res;
    });
  }

  private final JPanel myWholePanel = new JPanel(new BorderLayout());
  private final TextFieldWithBrowseButton myResources = new TextFieldWithBrowseButton();
  private final JPanel myResourcesPanel = new JPanel(new GridBagLayout());
  private final JCheckBox myResourcesEnable = new JCheckBox(J2MEBundle.message("resource.directory.create.command"));
  private boolean myModified = false;
  private final String myDefaultResourcesDir;

  @Override
  public String getDisplayName() {
    return J2MEBundle.message("resources.settings.title");
  }

  @Override
  public String getHelpTopic() {
    return "j2me.moduleJ2ME";
  }

  @Override
  public JComponent createComponent() {
    myResources.setText(myProperties.getResourcePath());

    myResourcesEnable.setSelected(myProperties.getResourcePath() != null);
    myResources.setEnabled(myResourcesEnable.isSelected());
    myResourcesEnable.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myResources.setEnabled(myResourcesEnable.isSelected());
        if (myResources.isEnabled()) {
          myResources.setText(myProperties.getResourcePath() != null ? myProperties.getResourcePath() : myDefaultResourcesDir);
        }
        myModified = true;
      }
    });
    myResources.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        myModified = true;
      }
    });
    myResources.addBrowseFolderListener(J2MEBundle.message("resource.directory.chooser"), J2MEBundle.message("resource.directory.chooser"), myModule.getProject(),
                                        new FileChooserDescriptor(false, true, false, true, false, false));
    myWholePanel.add(myResourcesEnable, BorderLayout.NORTH);
    myResourcesPanel.add(myResources,
                         new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHEAST,
                                                GridBagConstraints.HORIZONTAL, JBUI.insets(6, 12, 0, 2), 0, 0));
    JPanel resourcePanel = new JPanel(new BorderLayout());
    resourcePanel.add(myResourcesPanel, BorderLayout.NORTH);
    myWholePanel.add(resourcePanel, BorderLayout.CENTER);
    myWholePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    return myWholePanel;
  }

  @Override
  public boolean isModified() {
    return myModified;
  }

  @Override
  public void apply() throws ConfigurationException {
    if (myResourcesEnable.isSelected()) {
      if (myResources.getText() == null || myResources.getText().length() == 0) {
        throw new ConfigurationException(J2MEBundle.message("resource.directory.not.set"));
      }
      myProperties.setResourcePath(myResources.getText());
      if (!new File(myResources.getText()).exists()) {

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
            final String path = FileUtil.toSystemIndependentName(myResources.getText());
            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
            if (file == null) {
              final File ioFile = new File(myResources.getText());
              CommandProcessor.getInstance().executeCommand(myModule.getProject(), new Runnable() {
                @Override
                public void run() {
                  FileUtil.createParentDirs(ioFile);
                  final LocalFileSystem lfs = LocalFileSystem.getInstance();
                  final File ioFileParentFile = ioFile.getParentFile();
                  final VirtualFile dir = ioFileParentFile != null ? lfs.refreshAndFindFileByIoFile(ioFileParentFile) : null;
                  if (dir != null) {
                    try {
                      dir.createChildDirectory(this, ioFile.getName());
                    }
                    catch (IOException e) {
                      LOG.error(e);
                    }
                  }
                }
              }, J2MEBundle.message("resource.directory.create.command"), null);
            }
          }
        });
      }
      ApplicationManager.getApplication().runWriteAction(() -> {
        final String resourcesPath = myResources.getText();
        if (resourcesPath == null || resourcesPath.length() == 0) return;
        final String path = FileUtil.toSystemIndependentName(resourcesPath);
        ContentEntry contentEntry = null;
        for (ContentEntry entry : myRootModel.getContentEntries()) {
          final VirtualFile contentRoot = entry.getFile();
          if (contentRoot != null && path.startsWith(contentRoot.getPath())) {
            contentEntry = entry;
            break;
          }
        }
        if (contentEntry == null) {
          final VirtualFile ioFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(path).getParentFile());
          if (ioFile == null) return;
          contentEntry = myRootModel.addContentEntry(ioFile);
        }
        final VirtualFile res = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
        if (res != null) {
          contentEntry.addSourceFolder(res, false);
        }
      });
    }
    else {
      if (myProperties.getResourcePath() != null) {
        ApplicationManager.getApplication().runWriteAction(() -> {
          for (int i = 0; i < myRootModel.getContentEntries().length; i++) {
            ContentEntry contentEntry = myRootModel.getContentEntries()[i];
            for (SourceFolder folder : contentEntry.getSourceFolders()) {
              final VirtualFile sourceRoot = folder.getFile();
              if (sourceRoot != null &&
                  sourceRoot.getPath().compareTo(FileUtil.toSystemIndependentName(myProperties.getResourcePath())) == 0) {
                contentEntry.removeSourceFolder(folder);
              }
            }
          }
        });
      }
      myProperties.setResourcePath(null);
    }
    myModified = false;
  }

  @Override
  public void reset() {
    myResources.setText(myProperties.getResourcePath());
    myResourcesEnable.setSelected(myProperties.getResourcePath() != null);
    myModified = false;
  }

  @Override
  public void disposeUIResources() {}
}
