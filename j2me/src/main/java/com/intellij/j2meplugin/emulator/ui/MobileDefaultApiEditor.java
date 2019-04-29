/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator.ui;

import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.emulator.Emulator;
import com.intellij.j2meplugin.emulator.midp.MIDPEmulatorType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MobileDefaultApiEditor extends MobileApiSettingsEditor {
  private JTextField myProfile;
  private JTextField myConfiguration;
  private JPanel myMIDPPanel;
  private JRadioButton myDefaultConfigs;

  private JLabel myDefaultProfile;
  private JLabel myDefaultConfig;

  private JRadioButton myCustomConfigs;
  private static final Logger LOG = Logger.getInstance(MobileDefaultApiEditor.class);


  @Override
  @NotNull
  public JComponent createEditor() {
    ButtonGroup useConfigs = new ButtonGroup();
    useConfigs.add(myDefaultConfigs);
    useConfigs.add(myCustomConfigs);
    myDefaultConfigs.setSelected(true);
    ActionListener listener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myModified = true;
      }
    };
    myDefaultConfigs.addActionListener(listener);
    myCustomConfigs.addActionListener(listener);
    myProfile.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        myModified = true;
      }
    });
    myConfiguration.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        myModified = true;
      }
    });
    return myMIDPPanel;
  }

  @Override
  public void resetEditorFrom(@NotNull Emulator emulator) {
    final MIDPEmulatorType emulatorType = (MIDPEmulatorType)emulator.getEmulatorType();
    LOG.assertTrue(emulatorType != null);
    final String defaultProfile = emulatorType.getDefaultProfile(emulator.getHome());
    final String defaultConfiguration = emulatorType.getDefaultConfiguration(emulator.getHome());
    myDefaultProfile.setText(defaultProfile);
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
    //set default values to custom settings?!
    if (emulator.getCustomProfile() == null || emulator.getCustomProfile().length() == 0) {
      myProfile.setText(defaultProfile);
    }
    else {
      myProfile.setText(emulator.getCustomProfile());
    }
    if (emulator.getCustomConfiguration() == null || emulator.getCustomConfiguration().length() == 0) {
      myConfiguration.setText(defaultConfiguration);
    }
    else {
      myConfiguration.setText(emulator.getCustomConfiguration());
    }
    myModified = false;
  }

  @Override
  public void applyEditorTo(@NotNull Emulator emulator) throws ConfigurationException {
    if (myCustomConfigs.isSelected()) {
      if (myProfile.getText() == null || myProfile.getText().length() == 0) {
        throw new ConfigurationException(J2MEBundle.message("emulator.profile.version.misconfigured"));
      }
      emulator.setProfile(myProfile.getText());
      if (myConfiguration.getText() == null || myConfiguration.getText().length() == 0) {
        throw new ConfigurationException(J2MEBundle.message("emulator.configuration.version.misconfigured"));
      }
      emulator.setConfiguration(myConfiguration.getText());
    }
    else {
      emulator.setProfile(myDefaultProfile.getText());
      emulator.setConfiguration(myDefaultConfig.getText());
    }
    emulator.setCustomProfile(myProfile.getText());
    emulator.setCustomConfiguration(myConfiguration.getText());
    myModified = false;
  }
}
