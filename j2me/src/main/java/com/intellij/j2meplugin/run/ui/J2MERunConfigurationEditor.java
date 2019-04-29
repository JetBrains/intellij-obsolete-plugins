/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.run.ui;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.impl.CheckableRunConfigurationEditor;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.emulator.Emulator;
import com.intellij.j2meplugin.emulator.EmulatorType;
import com.intellij.j2meplugin.emulator.MobileSdk;
import com.intellij.j2meplugin.module.J2MEModuleProperties;
import com.intellij.j2meplugin.module.J2MEModuleType;
import com.intellij.j2meplugin.module.MobileModuleUtil;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.j2meplugin.module.settings.general.UserKeysConfigurable;
import com.intellij.j2meplugin.run.J2MERunConfiguration;
import com.intellij.j2meplugin.run.ui.editors.EmulatorEditor;
import com.intellij.j2meplugin.util.J2MEClassBrowser;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.RawCommandLineEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author anna
 */
public class J2MERunConfigurationEditor extends SettingsEditor<J2MERunConfiguration> implements CheckableRunConfigurationEditor<J2MERunConfiguration>{
  private EditorPanel myEditor;
  private final Project myProject;

  private final J2MERunConfiguration myJ2MERunConfiguration;
  private static final Logger LOG = Logger.getInstance(J2MERunConfigurationEditor.class);

  public J2MERunConfigurationEditor(Project project, J2MERunConfiguration j2MERunConfiguration) {
    myProject = project;
    myJ2MERunConfiguration = j2MERunConfiguration;
  }

  @Override
  public void resetEditorFrom(@NotNull J2MERunConfiguration j2merc) {
    myEditor.myModule.getComponent().setSelectedModule(j2merc.getModule());
    if (myEditor.getEmulatorRunOptions() != null) {
      myEditor.getEmulatorRunOptions().reset();
    }
    myEditor.myJad.setText(j2merc.JAD_NAME);
    myEditor.myClass.setText(j2merc.MAIN_CLASS_NAME);
    myEditor.myUseClasses.setSelected(j2merc.IS_CLASSES);
    myEditor.myUseJad.setSelected(!j2merc.IS_CLASSES);
    myEditor.myUserKeysConfigurable.setUserDefinedOptions(j2merc.userParameters);
    if (myEditor.myOTASettings != null) {
      myEditor.myOTASettings.resetEditorFrom(j2merc);
    }
    myEditor.myUseOTA.setSelected(j2merc.IS_OTA);
    myEditor.myProgramParameters.getComponent().setText(j2merc.COMMAND_LINE_PARAMETERS);
    myEditor.changeConfigurationTargetSelection();
  }

  @Override
  public void applyEditorTo(@NotNull J2MERunConfiguration j2merc) throws ConfigurationException {
    j2merc.setModule(myEditor.getModule());
    if (myEditor.getEmulatorRunOptions() != null) {
      myEditor.getEmulatorRunOptions().apply();
    }
    j2merc.JAD_NAME = myEditor.myJad.getText();
    j2merc.MAIN_CLASS_NAME = myEditor.myClass.getText();
    j2merc.IS_CLASSES = myEditor.myUseClasses.isSelected();
    if (myEditor.myUserKeysConfigurable != null) {
      myEditor.myUserKeysConfigurable.getTable().stopEditing();
      j2merc.userParameters = new ArrayList<>(myEditor.myUserKeysConfigurable.getUserDefinedOptions().getItems());
    }
    if (myEditor.myOTASettings != null) myEditor.myOTASettings.applyEditorTo(j2merc);
    j2merc.IS_OTA = myEditor.myUseOTA.isSelected();
    j2merc.COMMAND_LINE_PARAMETERS = myEditor.myProgramParameters.getComponent().getText();
  }

  @Override
  @NotNull
  public JComponent createEditor() {
    myEditor = new EditorPanel();
    return myEditor.getComponent();
  }

  @Override
  public void checkEditorData(final J2MERunConfiguration j2merc) {
    try {
      j2merc.setModule(myEditor.getModule());
      if (myEditor.getEmulatorRunOptions() != null) {
        myEditor.getEmulatorRunOptions().apply();
      }
      j2merc.JAD_NAME = myEditor.myJad.getText();
      j2merc.MAIN_CLASS_NAME = myEditor.myClass.getText();
      j2merc.IS_CLASSES = myEditor.myUseClasses.isSelected();
      if (myEditor.myOTASettings != null) myEditor.myOTASettings.applyEditorTo(j2merc);
      j2merc.IS_OTA = myEditor.myUseOTA.isSelected();
      j2merc.COMMAND_LINE_PARAMETERS = myEditor.myProgramParameters.getComponent().getText();
    }
    catch (ConfigurationException e) {
      //can't be
    }
  }

  private class EditorPanel implements PanelWithAnchor {
    private JPanel myWholePanel;
    private JComponent anchor;

    private LabeledComponent<ModulesComboBox> myModule;

    private JPanel myChoosenEmulatorSettingsPlace;
    private LabeledComponent<RawCommandLineEditor> myProgramParameters;
    private JPanel myConfigurationPanel;

    private JRadioButton myUseClasses;
    private JRadioButton myUseJad;
    private JRadioButton myUseOTA;

    private TextFieldWithBrowseButton myClass;
    private JLabel myClassLabel;
    private JPanel myClassPanel;

    private TextFieldWithBrowseButton myJad;
    private JLabel myJadLabel;
    private JPanel myJadPanel;

    private JPanel myOTAPanel;
    private JPanel myUserOptionsPanel;

    private UserKeysConfigurable myUserKeysConfigurable;

    private OTASettingsConfigurable myOTASettings;

    public EmulatorEditor getEmulatorRunOptions() {
      if (myEmulatorEditor == null) {
        fillEmulatorEditor(getModule());
      }
      return myEmulatorEditor;
    }

    private EmulatorEditor myEmulatorEditor;

    EditorPanel() {
      myConfigurationPanel.setVisible(false);
      myProgramParameters.setComponent(new RawCommandLineEditor());
      myProgramParameters.getComponent().setDialogCaption(myProgramParameters.getRawText());
      myModule.setComponent(new ModulesComboBox());
      myModule.getComponent().fillModules(myProject, J2MEModuleType.getInstance());
      myModule.getComponent().addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          moduleChanged();
        }
      });

      myChoosenEmulatorSettingsPlace.setLayout(new BorderLayout());

      myOTAPanel.setVisible(false);
      myUserOptionsPanel.setLayout(new BorderLayout());
      myUserOptionsPanel.setVisible(false);

      myUseOTA.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          changeConfigurationTargetSelection();
        }
      });
      myUseJad.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          changeConfigurationTargetSelection();
        }
      });
      myUseClasses.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          changeConfigurationTargetSelection();
        }
      });

      myClass.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (getModule() == null) {
            return;
          }
          final J2MEClassBrowser j2MEClassBrowser = new J2MEClassBrowser(getModule());
          j2MEClassBrowser.show();
          j2MEClassBrowser.setField(myClass);
        }

      });


      myJad.addBrowseFolderListener(J2MEBundle.message("run.configuration.browse.file"),
                                    J2MEBundle.message("run.configuration.file.to.start"),
                                    myProject,
                                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());


      myUseJad.setSelected(true);
      myClassPanel.setVisible(false);

      setAnchor(myModule.getLabel());
    }

    private void createKeysConfigurable() {
      myUserKeysConfigurable = new UserKeysConfigurable(new HashSet<>());
      myUserOptionsPanel.removeAll();
      myUserOptionsPanel.add(myUserKeysConfigurable.getUserKeysPanel(), BorderLayout.CENTER);
    }

    public JComponent getComponent() {
      fillEmulatorEditor(myJ2MERunConfiguration.getModule());
      createKeysConfigurable();
      JPanel myBorder = new JPanel(new BorderLayout());
      myBorder.add(myWholePanel, BorderLayout.CENTER);
      return myBorder;
    }

    private Module getModule() {
      return myModule.getComponent().getSelectedModule();
    }

    public void moduleChanged() {
      myConfigurationPanel.setVisible(false);
      fillEmulatorEditor(getModule());
      if (getModule() == null) return;
      final Sdk projectJdk = ModuleRootManager.getInstance(getModule()).getSdk();
      if (projectJdk == null || !MobileSdk.checkCorrectness(projectJdk, getModule())) return;
      myConfigurationPanel.setVisible(true);

      final String[] otaCommands = ((Emulator)projectJdk.getSdkAdditionalData()).getOTACommands(projectJdk.getHomePath());
      LOG.assertTrue(otaCommands != null);
      if (otaCommands.length > 0) {
        myOTASettings = new OTASettingsConfigurable(projectJdk, getModule());
        myOTASettings.setCommands(otaCommands);
        Disposer.register(J2MERunConfigurationEditor.this, myOTASettings);
        myUseOTA.setVisible(true);
      }
      else {
        myUseOTA.setVisible(false);
      }
      final EmulatorType emulatorType = MobileSdk.getEmulatorType(projectJdk, getModule());
      LOG.assertTrue(emulatorType != null);
      final MobileApplicationType mobileApplicationType = MobileModuleUtil.getMobileApplicationTypeByName(emulatorType.getApplicationType());
      LOG.assertTrue(mobileApplicationType != null);
      final String extension = mobileApplicationType.getExtension();
      myUseJad.setText(StringUtil.capitalize(extension));
      myJadLabel.setText(J2MEBundle.message("file.label", StringUtil.capitalize(extension)));
      myClassLabel.setText(StringUtil.capitalize(J2MEBundle.message("klass.label", mobileApplicationType.getPresentableClassName())));
      final MobileModuleSettings moduleSettings = MobileModuleSettings.getInstance(getModule());
      LOG.assertTrue(moduleSettings != null);
      myJad.setText(moduleSettings.getMobileDescriptionPath());
      myUseJad.setSelected(true);
      changeConfigurationTargetSelection();
    }

    private void changeConfigurationTargetSelection(){
      if (getModule() == null) return;
      myClassPanel.setVisible(myUseClasses.isSelected());
      myJadPanel.setVisible(myUseJad.isSelected());
      myUserOptionsPanel.setVisible(false);
      myOTAPanel.setVisible(false);
      if (myUseClasses.isSelected()){
        if (J2MEModuleProperties.getInstance(getModule()).getMobileApplicationType().isUserParametersEnable()) {
          myUserOptionsPanel.setVisible(true);
        }
        else {
          myUserOptionsPanel.setVisible(false);
        }
      }
      if (myUseJad.isSelected()) {
        myJadPanel.setBorder(IdeBorderFactory.createTitledBorder(myUseJad.getText().toUpperCase(), true));
      }
      if (myUseOTA.isSelected()) {
        myOTAPanel.removeAll();
        myOTAPanel.add(myOTASettings.createEditor(), BorderLayout.CENTER);
        myOTAPanel.setVisible(true);
      }
    }

    private void fillEmulatorEditor(final Module module) {
      if (module != null) {
        final Sdk projectJdk = ModuleRootManager.getInstance(module).getSdk();
        if (projectJdk != null && MobileSdk.checkCorrectness(projectJdk, getModule())) {
          myChoosenEmulatorSettingsPlace.removeAll();
          final Emulator emulator = (Emulator)projectJdk.getSdkAdditionalData();
          final EmulatorType emulatorType = emulator.getEmulatorType();
          LOG.assertTrue(emulatorType != null);
          myEmulatorEditor = new EmulatorEditor(myJ2MERunConfiguration, emulatorType.getAvailableSkins(projectJdk.getHomePath()), projectJdk);
          final JComponent emulatorEditor = myEmulatorEditor.createComponent();
          if (myEmulatorEditor.isVisible()) {
            myChoosenEmulatorSettingsPlace.add(emulatorEditor, BorderLayout.CENTER);
            myChoosenEmulatorSettingsPlace.setVisible(true);
          }
          else {
            myChoosenEmulatorSettingsPlace.setVisible(false);
          }
          myChoosenEmulatorSettingsPlace.updateUI();
        }
      } else {
        myChoosenEmulatorSettingsPlace.setVisible(false);
      }
    }

    @Override
    public JComponent getAnchor() {
      return anchor;
    }

    @Override
    public void setAnchor(JComponent anchor) {
      this.anchor = anchor;
      myProgramParameters.setAnchor(anchor);
      myModule.setAnchor(anchor);
    }
  }
}
