/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings;

import com.intellij.j2meplugin.compiler.MobileMakeUtil;
import com.intellij.j2meplugin.module.settings.general.UserDefinedOption;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public abstract class MobileSettingsConfigurable implements UnnamedConfigurable {
  protected MobileModuleSettings mySettings;
  private static final Logger LOG = Logger.getInstance("#com.intellij.j2meplugin");
  protected Module myModule;
  protected Project myProject;

  protected MobileSettingsConfigurable(Module module, final MobileModuleSettings settings, final Project project) {
    mySettings = settings;
    myModule = module;
    myProject = project;
  }


  public abstract void disableMidletProperties();

  @Override
  public void reset() {
    if (mySettings.isSynchronized()) {
      try {
        WriteAction.run(() -> {
          final VirtualFile descriptor = LocalFileSystem.getInstance().refreshAndFindFileByPath(
            mySettings.getMobileDescriptionPath().replace(File.separatorChar, '/'));
          final FileDocumentManager documentManager = FileDocumentManager.getInstance();
          if (descriptor != null) {
            final Document document = documentManager.getDocument(descriptor);
            if (document != null) {
              documentManager.saveDocument(document);
            }
          }
        });
        Properties properties = new Properties();
        final File descriptor = new File(mySettings.getMobileDescriptionPath());
        if (descriptor.exists()) {
          final InputStream is = new BufferedInputStream(new FileInputStream(descriptor));
          try {
            properties.load(is);
            mySettings.getSettings().clear();
            mySettings.getUserDefinedOptions().clear();
            for (final Object o : properties.keySet()) {
              String key = (String)o;
              if (mySettings.getApplicationType().isUserField(key)) {
                mySettings.getUserDefinedOptions().add(new UserDefinedOption(key, properties.getProperty(key)));
              }
              else {
                mySettings.getSettings().put(key, properties.getProperty(key));
              }
            }
          }
          finally {
            is.close();
          }
        }
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }
  }

  @Override
  public void apply() throws ConfigurationException {
    if (mySettings.isSynchronized()) {
      try {
        MobileMakeUtil.makeJad(mySettings, false);
      }
      catch (Exception e) {
        throw new ConfigurationException(e.getMessage());
      }
    }
  }


  public MobileModuleSettings getSettings() {
    return mySettings;
  }
}
