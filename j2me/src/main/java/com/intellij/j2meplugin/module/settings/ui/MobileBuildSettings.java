/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings.ui;

import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.module.J2MEModuleExtension;
import com.intellij.j2meplugin.module.J2MEModuleProperties;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MobileBuildSettings implements ModuleConfigurationEditor {
  private static final Logger LOG = Logger.getInstance("#com.intellij.j2meplugin");
  private final Module myModule;
  private final ModifiableRootModel myRootModel;
  private final MobileExplodedPanel myExplodedPanel;
  private final MobileBuildPanel myBuildPanel;
  private final MobileModuleResourcesSettings myResourcesSettings;

  public MobileBuildSettings(Module module, ModifiableRootModel rootModel) {
    myModule = module;
    myRootModel = rootModel;
    final J2MEModuleExtension extension = rootModel.getModuleExtension(J2MEModuleExtension.class);
    final VirtualFile explodedDirectory = extension.getExplodedDirectory();
    final String defaultExplodedPath =
      explodedDirectory != null ? explodedDirectory.getPath() : ReadAction.compute(() -> {
          @NonNls final String output = "output";
          return new File(myModule.getModuleFilePath()).getParent().replace('/', File.separatorChar) + File.separatorChar + output;
        });
    myExplodedPanel =
    new MobileExplodedPanel(explodedDirectory != null, extension.isExcludeExplodedDirectory(), defaultExplodedPath);
    final MobileModuleSettings mobileModuleSettings = MobileModuleSettings.getInstance(myModule);
    myBuildPanel = new MobileBuildPanel(J2MEModuleProperties.getInstance(myModule).getMobileApplicationType(),
                                        myModule.getProject(),
                                        mobileModuleSettings);
    myResourcesSettings = new MobileModuleResourcesSettings(module, rootModel);
  }

  @Override
  public String getDisplayName() {
    return J2MEBundle.message("mobile.build.settings.title");
  }

  @Override
  public String getHelpTopic() {
    return "j2me.moduleJ2ME";
  }

  @Override
  public JComponent createComponent() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.add(myBuildPanel.createComponent(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
                                                                     GridBagConstraints.HORIZONTAL, JBUI.insets(5, 0), 0, 0));
    panel.add(myExplodedPanel.getComponent(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
                                                                     GridBagConstraints.HORIZONTAL, JBUI.insets(5, 0), 0, 0));
    final JPanel resourcesPanel = (JPanel)myResourcesSettings.createComponent();
    resourcesPanel.setBorder(IdeBorderFactory.createTitledBorder(myResourcesSettings.getDisplayName(), true));
    panel.add(resourcesPanel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
                                                     GridBagConstraints.HORIZONTAL, JBUI.insets(5, 0), 0, 0));
    panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    return panel;
  }

  @Override
  public boolean isModified() {
    return myBuildPanel.isModified() || myExplodedPanel.isModified() || myResourcesSettings.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    myBuildPanel.apply();
    myExplodedPanel.apply();
    final J2MEModuleExtension extension = myRootModel.getModuleExtension(J2MEModuleExtension.class);
    if (myExplodedPanel.isPathEnabled()) {
      final String path = myExplodedPanel.getExplodedDir();
      if (!new File(path).exists()) {
        /* if (Messages.showYesNoDialog(myModule.getProject(),
                                         "Exploded directory " + path + " doesn't exist. \n " +
                                         "Would you like to create it and continue?",
                                         "Exploded directory doesn't exist.",
                                         null) != DialogWrapper.OK_EXIT_CODE) {
           return;
         }
   */
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
            String pathDir = FileUtil.toSystemIndependentName(path);
            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(pathDir);
            if (file == null) {
              final File ioFile = new File(path);
              CommandProcessor.getInstance().executeCommand(myModule.getProject(), new Runnable() {
                @Override
                public void run() {
                  FileUtil.createParentDirs(ioFile);
                  final VirtualFile dir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile.getParentFile());
                  if (dir != null) {
                    try {
                      dir.createChildDirectory(this, ioFile.getName());
                    }
                    catch (IOException e) {
                      LOG.error(e);
                    }
                  }
                }
              }, J2MEBundle.message("exploded.directory.create.command"), null);
            }
          }
        });
      }

      String canonicalPath;
      try {
        canonicalPath = path.isEmpty() ? null : new File(path).getCanonicalPath();
      }
      catch (IOException e) {
        canonicalPath = path;
      }

      final String url = (canonicalPath == null)
                         ? null
                         : VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, canonicalPath.replace(File.separatorChar, '/'));
      extension.setExplodedDirectory(url);
      extension.setExcludeExplodedDirectory(myExplodedPanel.isExcludeFromContent());
    }
    else {
      extension.setExplodedDirectory((VirtualFile)null);
    }
    myResourcesSettings.apply();
  }

  @Override
  public void reset() {
    myBuildPanel.reset();
    myExplodedPanel.reset();
    myResourcesSettings.reset();
  }

  @Override
  public void disposeUIResources
    () {
  }
}
