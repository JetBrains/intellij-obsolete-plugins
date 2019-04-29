/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings.ui;

import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.FieldPanel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class MobileExplodedPanel {


  private final JPanel myExplodedPanel;
  private final JLabel myExplanation;
  private final JCheckBox myUseExplodedDir;
  private final FieldPanel myExplodedDirectory;

  private String myExplodedPath;
  private boolean myUseExplodedDirectory;
  private boolean myExcludeFromContent;
  private final JCheckBox myExcludeFromModuleContent;
  private boolean myModified = false;

  //private JPanel myRemoteDeployPanel;

  public MobileExplodedPanel(boolean useExplodedDir, boolean excludeFromContent, String explodedDir) {


    myExplodedPanel = new JPanel(new GridBagLayout());
    myExplodedPanel.setBorder(IdeBorderFactory.createTitledBorder(J2MEBundle.message("exploded.directory.settings"),
                                                                  true));
    myExplanation = new JLabel(J2MEBundle.message("exploded.directory.explanation"));
    myUseExplodedDir = new JCheckBox(J2MEBundle.message("exploded.directory.setup"));
    myExplodedDirectory = new FieldPanel();
    myExcludeFromModuleContent = new JCheckBox(J2MEBundle.message("exploded.directory.excluded"));
    myExplodedPanel.add(myExplanation,
                        new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
                                               GridBagConstraints.HORIZONTAL, JBUI.insets(5, 0), 0, 0));
    myExplodedPanel.add(myUseExplodedDir,
                        new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
                                               GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0));
    myExplodedPanel.add(myExplodedDirectory,
                        new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
                                               GridBagConstraints.HORIZONTAL, JBUI.insets(5, 13, 0, 0), 0, 0));
    myExplodedPanel.add(myExcludeFromModuleContent,
                        new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
                                               GridBagConstraints.NONE, JBUI.insetsLeft(10), 0, 0));

    /*   myRemoteDeployPanel = new MobileRemoteDeploymentPanel().getComponent();
       myRemoteDeployPanel.setBorder(IdeBorderFactory.createBoldTitledBorder("Deployment to remote server"));
       myExplodedPanel.add(myRemoteDeployPanel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
                                               GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
   */
    myExplodedPath = explodedDir;
    myUseExplodedDirectory = useExplodedDir;
    myExcludeFromContent = excludeFromContent;
  }

  public String getExplodedDir() {
    return myExplodedPath;
  }

  public boolean isExcludeFromContent() {
    return myExcludeFromContent;
  }

  public boolean isPathEnabled() {
    return myUseExplodedDirectory;
  }

  public JComponent getComponent() {

   myUseExplodedDir.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myUseExplodedDir.isSelected()) {
          if (myExplodedDirectory.getText() == null || myExplodedDirectory.getText().equals("")) {
            myExplodedDirectory.setText(myExplodedPath);
          }
          myExplodedDirectory.setEnabled(true);
          myExcludeFromModuleContent.setEnabled(true);
        }
        else {
          myExplodedDirectory.setEnabled(false);
          myExcludeFromModuleContent.setEnabled(false);
        }
        myModified = true;
      }
    });
    final LocalFileSystem lfs = LocalFileSystem.getInstance();
    myExplodedDirectory.setBrowseButtonActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        descriptor.setTitle(J2MEBundle.message("exploded.directory.chooser.title"));
        descriptor.setDescription(J2MEBundle.message("exploded.directory.chooser"));
        VirtualFile toSelect = null;
        if (myExplodedDirectory.getText() != null){
          toSelect = lfs.findFileByPath(myExplodedDirectory.getText().replace(File.separatorChar, '/'));
        }
        VirtualFile file = FileChooser.chooseFile(descriptor, myExplodedPanel, null, toSelect);
        if (file != null) {
          myExplodedDirectory.setText(FileUtil.toSystemDependentName(file.getPath()));
        }
      }
    });
    myExplodedDirectory.createComponent();
    myExplodedDirectory.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        myModified = true;
      }
    });

    myExcludeFromModuleContent.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myModified = true;
      }
    });
    return myExplodedPanel;
  }

  public JComponent getPreferredFocusedComponent() {
    return myUseExplodedDir;
  }

  public void reset(){
    if (myUseExplodedDirectory) {
      myExplodedDirectory.setText(myExplodedPath);
      myExcludeFromModuleContent.setSelected(myExcludeFromContent);
    }
    myExplodedDirectory.setEnabled(myUseExplodedDirectory);
    myExcludeFromModuleContent.setEnabled(myUseExplodedDirectory);
    myUseExplodedDir.setSelected(myUseExplodedDirectory);
    myModified = false;
  }

  public void apply(){
    myUseExplodedDirectory = myUseExplodedDir.isSelected();
    if (myUseExplodedDirectory){
      myExplodedPath = myExplodedDirectory.getText();
    } else {
      myExplodedPath = null;
    }
    myExcludeFromContent = myExcludeFromModuleContent.isSelected();
    myModified = false;
  }

  public boolean isModified(){
    return myModified;
  }
}
