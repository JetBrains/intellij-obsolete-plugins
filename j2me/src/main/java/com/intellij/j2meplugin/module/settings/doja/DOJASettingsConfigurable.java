/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings.doja;

import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.j2meplugin.module.settings.MobileSettingsConfigurable;
import com.intellij.j2meplugin.util.J2MEClassBrowser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

public class DOJASettingsConfigurable extends MobileSettingsConfigurable {
  private JPanel myWholePanel;
  private JTextField myApplicationName;
  private JLabel myApplicationNameLabel;

  private TextFieldWithBrowseButton myPackageUrl;
  private JLabel myPackageLabel;


  private TextFieldWithBrowseButton myApplicationClass;
  private JLabel myApplicationClassLabel;

  private boolean myModified = false;

  private JButton myOptionsButton;

  private final HashMap<String, String> myTempSettings;

  public DOJASettingsConfigurable(Module module, MobileModuleSettings settings, Project project) {
    super(module, settings, project);
    myTempSettings = new HashMap<>();
  }

  @Override
  public JComponent createComponent() {
    ActionListener modifier = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myModified = true;
      }
    };

    DocumentAdapter textModifier = new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        myModified = true;
      }
    };

    DocumentAdapter defaultModifier = new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        mySettings.setDefaultModified(true);
      }
    };

    myApplicationNameLabel.setText(DOJAApplicationType.APPLICATION_NAME + ":");
    myApplicationName.getDocument().addDocumentListener(textModifier);
    myApplicationName.getDocument().addDocumentListener(defaultModifier);

    myApplicationClassLabel.setText(DOJAApplicationType.APPLICATION_CLASS + ":");
    myApplicationClass.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        J2MEClassBrowser j2MEClassBrowser = new J2MEClassBrowser(myModule);
        j2MEClassBrowser.show();
        j2MEClassBrowser.setField(myApplicationClass);
        myModified = true;
      }
    });
    myApplicationClass.getTextField().getDocument().addDocumentListener(textModifier);

    myPackageLabel.setText(DOJAApplicationType.PACKAGE_URL + ":");
    myPackageUrl.addBrowseFolderListener(J2MEBundle.message("build.settings.jar.utl.title"),
                                         J2MEBundle.message("build.settings.jar.url"),
                                         myProject,
                                         new FileChooserDescriptor(true,
                                                                   true,
                                                                   true,
                                                                   true,
                                                                   false,
                                                                   false));
    myPackageUrl.addActionListener(modifier);
    myPackageUrl.getTextField().getDocument().addDocumentListener(textModifier);
    myPackageUrl.getTextField().getDocument().addDocumentListener(defaultModifier);

    myOptionsButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        new OptionalDojaSettingsEditor(myWholePanel, myTempSettings).show();
        myModified = true;
      }
    });

    return myWholePanel;
  }

  @Override
  public boolean isModified() {
    return myModified;
  }

  @Override
  public void apply() throws ConfigurationException {
    if (myApplicationName.getText() == null || myApplicationName.getText().length() == 0) {
      throw new ConfigurationException(J2MEBundle.message("module.settings.doja.application.not.specified"));
    }

    if (myPackageUrl.getText() == null || myPackageUrl.getText().length() == 0) {
      throw new ConfigurationException(J2MEBundle.message("compiler.jar.file.not.specified"));
    }
    mySettings.getSettings().clear();
    for (final String key : myTempSettings.keySet()) {
      mySettings.putSetting(key, myTempSettings.get(key));
    }
    mySettings.putSetting(DOJAApplicationType.APPLICATION_NAME, myApplicationName.getText());
    mySettings.putSetting(DOJAApplicationType.PACKAGE_URL, myPackageUrl.getText());
    mySettings.putSetting(DOJAApplicationType.APPLICATION_CLASS, myApplicationClass.getText());

    super.apply();
    myModified = false;
    mySettings.setDefaultModified(false);
  }

  @Override
  public void reset() {
    super.reset();
    myApplicationName.setText(mySettings.getSettings().get(DOJAApplicationType.APPLICATION_NAME));
    myApplicationClass.setText(mySettings.getSettings().get(DOJAApplicationType.APPLICATION_CLASS));
    myPackageUrl.setText(mySettings.getSettings().get(DOJAApplicationType.PACKAGE_URL));
    myModified = false;
    mySettings.setDefaultModified(false);
    myTempSettings.putAll(mySettings.getSettings());
  }

  @Override
  public void disposeUIResources() {}

  @Override
  public void disableMidletProperties() {
    myApplicationClassLabel.setVisible(false);
    myApplicationClass.setVisible(false);
  }

}
