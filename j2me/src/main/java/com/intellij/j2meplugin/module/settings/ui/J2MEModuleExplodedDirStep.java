/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings.ui;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.j2meplugin.module.J2MEModuleBuilder;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class J2MEModuleExplodedDirStep extends ModuleWizardStep {
  private final J2MEModuleBuilder myModuleBuilder;

  private final Icon myIcon;
  private final String myHelpId;
  private MobileExplodedPanel myExplodedPanel;
  private MobileBuildPanel myBuildPanel;
  private String myDefaultExplodedPath;

  public J2MEModuleExplodedDirStep(WizardContext wizardContext,
                                   J2MEModuleBuilder moduleBuilder,
                                   Icon wizardIcon,
                                   @NonNls String s) {

    myModuleBuilder = moduleBuilder;
    myIcon = wizardIcon;
    myHelpId = s;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myBuildPanel.getPreferredFocusedComponent();
  }

  @Override
  public JComponent getComponent() {
    @NonNls final String output = "output";
    myDefaultExplodedPath = myModuleBuilder.getModuleFileDirectory() != null ?
                            myModuleBuilder.getModuleFileDirectory().replace('/', File.separatorChar) + File.separator +
                            output : "";
    myExplodedPanel =
    new MobileExplodedPanel(myModuleBuilder.getExplodedDirPath() != null,
                            myModuleBuilder.isExcludeFromContent(),
                            myModuleBuilder.isDefaultEDirectoryModified() && myModuleBuilder.getExplodedDirPath() != null
                            ? myModuleBuilder.getExplodedDirPath()
                            : myDefaultExplodedPath);
    myBuildPanel = new MobileBuildPanel(myModuleBuilder.getMobileApplicationType(),
                                        null,
                                        myModuleBuilder.getMobileModuleSettings());
    JPanel myWholePanel = new JPanel(new GridBagLayout());
    myWholePanel.add(myBuildPanel.createComponent(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
                                                                            GridBagConstraints.HORIZONTAL, JBUI.insets(5), 0, 0));
    myWholePanel.add(myExplodedPanel.getComponent(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
                                                                            GridBagConstraints.HORIZONTAL, JBUI.insets(5), 0, 0));
    myWholePanel.setBorder(BorderFactory.createEtchedBorder());
    return myWholePanel;
  }

  @Override
  public void updateDataModel() {
    storeDifference();
  }

  @Override
  public boolean validate() throws ConfigurationException {
    myBuildPanel.apply();
    return true;
  }

  @Override
  public Icon getIcon() {
    return myIcon;
  }

  @Override
  public String getHelpId() {
    return myHelpId;
  }

  @Override
  public void onStepLeaving() {
    myExplodedPanel.apply();
    storeDifference();
    try {
      myBuildPanel.apply();
    }
    catch (ConfigurationException e) {
      //ignore
    }

  }

  private void storeDifference() {
    if (myExplodedPanel.isPathEnabled()) {
      myModuleBuilder.setExplodedDirPath(myExplodedPanel.getExplodedDir());
      myModuleBuilder.setExcludeFromContent(myExplodedPanel.isExcludeFromContent());
      myModuleBuilder.setDefaultEDirectoryModified(!myDefaultExplodedPath.equals(myExplodedPanel.getExplodedDir()));
    }
    else {
      myModuleBuilder.setExplodedDirPath(null);
    }
  }
}
