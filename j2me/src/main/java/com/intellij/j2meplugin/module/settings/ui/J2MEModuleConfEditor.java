/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings.ui;

import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.module.J2MEModuleProperties;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.j2meplugin.module.settings.MobileSettingsConfigurable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;


public class J2MEModuleConfEditor implements ModuleConfigurationEditor {
  private JPanel myWholePanel;
  private JPanel mySettingsPanel;
  private JCheckBox myUseUserDefinedJad;
  private MobileSettingsConfigurable mySettingsConfigurable;
  private MobileModuleSettings myModuleSettings;
  private final Module myModule;
  private final Project myProject;

  public J2MEModuleConfEditor(Module module, Project project) {
    myModule = module;
    myProject = project;
  }

  @Override
  public void disposeUIResources() {
    if (mySettingsConfigurable != null) {
      mySettingsConfigurable.disposeUIResources();
    }
  }

  public MobileApplicationType getApplicationType(Module module) {
    return J2MEModuleProperties.getInstance(module).getMobileApplicationType();
  }

  public MobileModuleSettings getModuleSettings(Module module) {
    return MobileModuleSettings.getInstance(module);
  }

  @Override
  public void reset() {
    mySettingsConfigurable.reset();
    myUseUserDefinedJad.setSelected(myModuleSettings.isSynchronized());
  }

  @Override
  public void apply() throws ConfigurationException {
    myModuleSettings.setSynchronized(myUseUserDefinedJad.isSelected());
    mySettingsConfigurable.apply();
  }

  @Override
  public boolean isModified() {
    return myUseUserDefinedJad.isSelected() != myModuleSettings.isSynchronized() || mySettingsConfigurable.isModified();
  }

  @Override
  public String getHelpTopic() {
    return "j2me.support.mobile.module.settings";
  }

  @Override
  public String getDisplayName() {
    return J2MEBundle.message("mobile.module.settings.title");
  }

  @Override
  public JPanel createComponent() {
    myModuleSettings = getModuleSettings(myModule);
    final MobileApplicationType applicationType = getApplicationType(myModule);

    mySettingsConfigurable = applicationType.createConfigurable(myProject, myModule, myModuleSettings);
    mySettingsPanel.removeAll();
    mySettingsPanel.add(mySettingsConfigurable.createComponent(), BorderLayout.CENTER);

    myUseUserDefinedJad.setText(J2MEBundle.message("module.settings.synchronization.needed", applicationType.getExtension().toUpperCase()));
    myUseUserDefinedJad.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myUseUserDefinedJad.isSelected() && new File(mySettingsConfigurable.getSettings().getMobileDescriptionPath()).exists()) {
          ApplicationManager.getApplication().runWriteAction(() -> {
            final VirtualFile descriptor = LocalFileSystem.getInstance()
              .refreshAndFindFileByPath(mySettingsConfigurable.getSettings().getMobileDescriptionPath().replace(File.separatorChar, '/'));
            final FileDocumentManager documentManager = FileDocumentManager.getInstance();
            documentManager.saveDocument(documentManager.getDocument(descriptor));
          });
        }
      }
    });    
    return myWholePanel;
  }

  public void disableMidletProperties() {
    mySettingsConfigurable.disableMidletProperties();
  }
}

