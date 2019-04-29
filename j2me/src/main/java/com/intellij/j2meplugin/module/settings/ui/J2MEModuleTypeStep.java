
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.module.settings.ui;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.j2meplugin.module.J2MEModuleBuilder;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;

public class J2MEModuleTypeStep extends ModuleWizardStep {
  private final J2MEModuleBuilder myBuilder;
  private final Icon myWizardIcon;
  private final String myHelpId;
  final J2MEModuleConfEditor myModuleConfEditor;


  public J2MEModuleTypeStep(J2MEModuleBuilder builder, final Icon wizardIcon, @NonNls final String helpId) {
    myBuilder = builder;
    myWizardIcon = wizardIcon;
    myHelpId = helpId;
    myModuleConfEditor = new J2MEModuleConfEditor(null, null) {
      @Override
      public MobileApplicationType getApplicationType(final Module module) {
        return myBuilder.getMobileApplicationType();
      }

      @Override
      public MobileModuleSettings getModuleSettings(final Module module) {
        return myBuilder.getMobileModuleSettings();
      }
    };
  }

  @Override
  public JComponent getComponent() {
    final JPanel panel = myModuleConfEditor.createComponent();
    myModuleConfEditor.disableMidletProperties();
    myModuleConfEditor.reset();
    return panel;
  }

  @Override
  public void updateDataModel() {
    try {
      myModuleConfEditor.apply();
    }
    catch (ConfigurationException e) {
      //can't be
    }
  }


  @Override
  public void onStepLeaving() {
    updateDataModel();
  }

  @Override
  @NonNls
  public String getHelpId() {
    return myHelpId;
  }


  @Override
  public Icon getIcon() {
    return myWizardIcon;
  }
}
