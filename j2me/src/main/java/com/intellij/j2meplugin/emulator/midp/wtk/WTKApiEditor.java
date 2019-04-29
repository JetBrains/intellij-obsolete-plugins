/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator.midp.wtk;

import com.intellij.j2meplugin.emulator.Emulator;
import com.intellij.j2meplugin.emulator.MobileSdkUtil;
import com.intellij.j2meplugin.emulator.midp.MIDPEmulatorType;
import com.intellij.j2meplugin.emulator.ui.MobileApiSettingsEditor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

class WTKApiEditor extends MobileApiSettingsEditor {
  private JComboBox myProfile;
  private final DefaultComboBoxModel myProfilesModel = new DefaultComboBoxModel();
  private JComboBox myConfiguration;
  private final DefaultComboBoxModel myConfigurationsModel = new DefaultComboBoxModel();
  private JPanel myMIDPPanel;
  private JRadioButton myDefaultConfigs;
  private JRadioButton myCustomConfigs;
  private JLabel myDefaultProfile;
  private JLabel myDefaultConfig;

  private final MIDPEmulatorType myMIDPType;
  private final Sdk mySdk;
  private final SdkModificator mySdkModificator;
  private static final Logger LOG = Logger.getInstance(WTKApiEditor.class);

  WTKApiEditor(MIDPEmulatorType type, Sdk sdk, SdkModificator sdkModificator) {
    myMIDPType = type;
    mySdk = sdk;
    mySdkModificator = sdkModificator;
  }

  @Override
  @NotNull
  public JComponent createEditor() {
    myProfile.setModel(myProfilesModel);
    myConfiguration.setModel(myConfigurationsModel);
    ButtonGroup useConfigs = new ButtonGroup();
    useConfigs.add(myDefaultConfigs);
    useConfigs.add(myCustomConfigs);
    myDefaultConfigs.setSelected(true);

    //custom profiles choices
    myProfilesModel.removeAllElements();
    final String[] profiles = myMIDPType.getAvailableProfiles(mySdk.getHomePath());
    for (int i = 0; profiles != null && i < profiles.length; i++) {
      myProfilesModel.addElement(profiles[i]);
    }
    //custom configurations choices
    myConfigurationsModel.removeAllElements();
    final String[] configurations = myMIDPType.getAvailableConfigurations(mySdk.getHomePath());
    for (int i = 0; configurations != null && i < configurations.length; i++) {
      myConfigurationsModel.addElement(configurations[i]);
    }

    myDefaultConfigs.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!myDefaultConfigs.isSelected()) return;
        doStateChanged(true);
        myModified = true;
      }
    });
    ActionListener changeRoots = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!myCustomConfigs.isSelected()) return;
        doStateChanged(false);
        myModified = true;
      }
    };
    myCustomConfigs.addActionListener(changeRoots);
    myProfile.addActionListener(changeRoots);
    myConfiguration.addActionListener(changeRoots);
    return myMIDPPanel;
  }

  private void doStateChanged(boolean isDefault) {
    final VirtualFile[] jarsToRemove = getJarsToRemove();
    if (jarsToRemove != null) {
      for (VirtualFile jarToRemove : jarsToRemove) {
        mySdkModificator.removeRoot(jarToRemove, OrderRootType.CLASSES);
      }
    }
    final VirtualFile[] jarsToAdd = getJarsToAdd(isDefault);
    if (jarsToAdd != null) {
      for (VirtualFile jarToAdd : jarsToAdd) {
        mySdkModificator.addRoot(jarToAdd, OrderRootType.CLASSES);
      }
    }
  }

  @Override
  public void resetEditorFrom(@NotNull Emulator emulator) {
    //from emulator settings
    final MIDPEmulatorType midpEmulatorType = ((MIDPEmulatorType)emulator.getEmulatorType());
    LOG.assertTrue(midpEmulatorType != null);
    final String defaultProfile = midpEmulatorType.getDefaultProfile(emulator.getHome());
    myDefaultProfile.setText(defaultProfile);
    final String defaultConfiguration = midpEmulatorType.getDefaultConfiguration(emulator.getHome());
    myDefaultConfig.setText(defaultConfiguration);
    if ((Comparing.equal(defaultProfile, emulator.getProfile()) &&
         Comparing.equal(defaultConfiguration, emulator.getConfiguration())) ||
                                                                      emulator.getProfile() == null ||
                                                                      emulator.getConfiguration() == null) {
      myDefaultConfigs.setSelected(true);
    }
    else {
      myCustomConfigs.setSelected(true);
    }
    myProfile.setSelectedItem(emulator.getCustomProfile());
    myConfiguration.setSelectedItem(emulator.getCustomConfiguration());
    myModified = false;
  }

  @Override
  public void applyEditorTo(@NotNull Emulator emulator) throws ConfigurationException {
    if (myCustomConfigs.isSelected()) {
      emulator.setProfile((String)myProfile.getSelectedItem());
      emulator.setConfiguration((String)myConfiguration.getSelectedItem());
    }
    else {
      emulator.setProfile(myDefaultProfile.getText());
      emulator.setConfiguration(myDefaultConfig.getText());
    }
    emulator.setCustomProfile((String)myProfile.getSelectedItem());
    emulator.setCustomConfiguration((String)myConfiguration.getSelectedItem());
    myModified = false;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private VirtualFile getJarBySettingName(String settingName, String homePath) {
    Properties apiSettings = ConfigurationUtil.getApiSettings(homePath);
    if (apiSettings == null || apiSettings.isEmpty()) {
      return null;
    }
    String jar = apiSettings.getProperty(settingName.replaceAll("-","") + ".file");//todo
    if (jar != null) {
      final VirtualFile[] apiClasses = MobileSdkUtil.findApiClasses(new String[]{homePath + File.separator + "lib" + File.separator + jar});
      if (apiClasses == null || apiClasses.length == 0) {
        return null;
      }
      return apiClasses[0];
    }
    return null;
  }

  private VirtualFile[] getJarsToRemove() {
    final String homePath = mySdk.getHomePath();
    ArrayList<VirtualFile> result = new ArrayList<>();
    for (int i = 0; i < myProfilesModel.getSize(); i++) {
      result.add(getJarBySettingName((String)myProfilesModel.getElementAt(i), homePath));
    }
    for (int i = 0; i < myConfigurationsModel.getSize(); i++) {
      result.add(getJarBySettingName((String)myConfigurationsModel.getElementAt(i), homePath));
    }
    ContainerUtil.addAll(result, getJarsToAdd(true));
    return VfsUtil.toVirtualFileArray(result);
  }

  private VirtualFile[] getJarsToAdd(boolean isDefault) {
    final String homePath = mySdk.getHomePath();
    if (isDefault) {
      return new VirtualFile[]{getJarBySettingName(myDefaultProfile.getText(), homePath),
          getJarBySettingName(myDefaultConfig.getText(), homePath)};
    }
    else {
      if (myProfile.getSelectedItem() == null || myConfiguration.getSelectedItem() == null) {
        return null;
      }
      return new VirtualFile[]{getJarBySettingName((String)myProfile.getSelectedItem(), homePath),
          getJarBySettingName((String)myConfiguration.getSelectedItem(), homePath)};
    }
  }

}
