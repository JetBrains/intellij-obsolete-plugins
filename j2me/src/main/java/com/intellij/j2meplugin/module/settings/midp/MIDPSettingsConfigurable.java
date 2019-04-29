/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings.midp;


import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.module.J2MEModuleProperties;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.j2meplugin.module.settings.MobileSettingsConfigurable;
import com.intellij.j2meplugin.module.settings.general.UserDefinedOption;
import com.intellij.j2meplugin.module.settings.general.UserKeysConfigurable;
import com.intellij.j2meplugin.util.J2MEClassBrowser;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;


public class MIDPSettingsConfigurable extends MobileSettingsConfigurable {
  private JTextField myMIDletName;
  private JLabel myMIDletNameLabel;

  private TextFieldWithBrowseButton myMIDletJarUrl;
  private JLabel myMIDletJarUrlLabel;


  private JTextField myMIDletVersion;
  private JLabel myMIDletVersionLabel;

  private JTextField myMIDletVendor;
  private JLabel myMIDletVendorLabel;


  private JButton myAddButton;
  private JButton myEditButton;
  private JButton myRemoveButton;
  private JButton myMoveUpButton;
  private JButton myMoveDownButton;
  private JButton myOptionalSettingsButton;

  private final DefaultListModel myListModel = new DefaultListModel();
  private JList myMIDletList;

  private JPanel myWholePanel;

  private JPanel myMIDletPropertiesPanel;

  private boolean myModified = false;
  private final HashMap<String, String> myTempSettings;
  private final HashSet<UserDefinedOption> myTempUserOptions;

  public MIDPSettingsConfigurable(Module module, MobileModuleSettings settings, Project project) {
    super(module, settings, project);
    myTempSettings = new HashMap<>();
    myTempUserOptions = new HashSet<>();
  }

  @Override
  public JPanel createComponent() {
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

    myMIDletNameLabel.setText(MIDPApplicationType.MIDLET_NAME + ":");
    myMIDletName.getDocument().addDocumentListener(textModifier);
    myMIDletName.getDocument().addDocumentListener(defaultModifier);

    myMIDletVersionLabel.setText(MIDPApplicationType.MIDLET_VERSION + ":");
    myMIDletVersion.getDocument().addDocumentListener(textModifier);
    myMIDletVersion.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        myMIDletVersion.selectAll();
      }

      @Override
      public void focusLost(FocusEvent e) {
      }
    });

    myMIDletVendorLabel.setText(MIDPApplicationType.MIDLET_VENDOR + ":");
    myMIDletVendor.getDocument().addDocumentListener(textModifier);
    myMIDletVendor.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        myMIDletVendor.selectAll();
      }

      @Override
      public void focusLost(FocusEvent e) {
      }
    });


    myMIDletJarUrlLabel.setText(MIDPApplicationType.MIDLET_JAR_URL + ":");
    myMIDletJarUrl.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
        fileChooserDescriptor.setTitle(J2MEBundle.message("build.settings.jar.utl.title"));
        fileChooserDescriptor.setDescription(J2MEBundle.message("build.settings.jar.url"));
        String directoryName = myMIDletJarUrl.getText().trim();
        VirtualFile initialFile = LocalFileSystem.getInstance().findFileByPath(directoryName.replace(File.separatorChar, '/'));
        VirtualFile file = FileChooser.chooseFile(fileChooserDescriptor, myProject, initialFile);
        if (file != null) {
          myMIDletJarUrl.setText(FileUtil.toSystemIndependentName(file.getPresentableUrl()));
        }
      }
    });
    myMIDletJarUrl.addActionListener(modifier);
    myMIDletJarUrl.getTextField().getDocument().addDocumentListener(textModifier);
    myMIDletJarUrl.getTextField().getDocument().addDocumentListener(defaultModifier);

    myMoveUpButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myMIDletList.getSelectedIndex() <= 0) return;
        moveMIDlet(-1);
        myModified = true;
      }
    });

    myMoveDownButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myMIDletList.getSelectedValue() == null || myMIDletList.getSelectedIndex() >= myListModel.size() - 1) return;
        moveMIDlet(+1);
        myModified = true;
      }
    });


    myMIDletList.setModel(myListModel);
    myMIDletList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myMIDletList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList jList, Object o, int i, boolean b, boolean b1) {
        super.getListCellRendererComponent(jList, o, i, b, b1);
        setText(((MIDPSettings.MIDletProperty)o).getName());
        return this;
      }
    });

    myRemoveButton.setEnabled(false);
    myEditButton.setEnabled(false);
    myMoveUpButton.setEnabled(false);
    myMoveDownButton.setEnabled(false);

    myMIDletList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (myMIDletList.getSelectedIndex() == -1) {
          myRemoveButton.setEnabled(false);
          myEditButton.setEnabled(false);
          myMoveUpButton.setEnabled(false);
          myMoveDownButton.setEnabled(false);
        }
        else {
          myEditButton.setEnabled(true);
          myRemoveButton.setEnabled(true);
          if (myMIDletList.getSelectedIndex() != 0) {
            myMoveUpButton.setEnabled(true);
          }
          else {
            myMoveUpButton.setEnabled(false);
          }
          if (myMIDletList.getSelectedIndex() != myListModel.getSize() - 1) {
            myMoveDownButton.setEnabled(true);
          }
          else {
            myMoveDownButton.setEnabled(false);
          }
        }
      }
    });

    myRemoveButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myMIDletList.getSelectedValue() == null) return;
        MIDPSettings.MIDletProperty midlet = (MIDPSettings.MIDletProperty)myMIDletList.getSelectedValue();
        ArrayList<MIDPSettings.MIDletProperty> midlets = new ArrayList<>();
        for (int i = 0; i < myListModel.size(); i++) {
          final MIDPSettings.MIDletProperty property = (MIDPSettings.MIDletProperty)myListModel.get(i);
          if (!property.equals(midlet)){
            midlets.add(property);
          }
        }
        myListModel.clear();
        int midletCount = 1;
        for (MIDPSettings.MIDletProperty key : midlets) {
          final MIDPSettings.MIDletProperty property = new MIDPSettings.MIDletProperty(MIDPApplicationType.MIDLET_PREFIX + midletCount,
                                                                                       key.getValueString());
          myListModel.addElement(property);
          midletCount++;
        }
        myModified = true;
      }
    });

    myAddButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        new MIDletOptionsPanel(myWholePanel,
                               new MIDPSettings.MIDletProperty("", "", "",
                                                               MIDPApplicationType.MIDLET_PREFIX + (myListModel.size() + 1)),
                               false).show();
      }
    });

    myEditButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myMIDletList.getSelectedValue() == null) return;
        new MIDletOptionsPanel(myWholePanel,
                               (MIDPSettings.MIDletProperty)myMIDletList.getSelectedValue(),
                               true).show();
      }
    });

    myOptionalSettingsButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        new AdditionalOptionsPanel(myWholePanel).show();
      }
    });
    myOptionalSettingsButton.setVisible(myModule != null);
    return myWholePanel;
  }

  @Override
  public void disableMidletProperties() {
    myMIDletPropertiesPanel.setVisible(false);
  }

  private void moveMIDlet(int direction) {
    int index = myMIDletList.getSelectedIndex();
    MIDPSettings.MIDletProperty from = (MIDPSettings.MIDletProperty)myListModel.getElementAt(index);
    MIDPSettings.MIDletProperty to = (MIDPSettings.MIDletProperty)myListModel.elementAt(index + direction);
    myListModel.set(index, new MIDPSettings.MIDletProperty(to.getName(),
                                                           to.getIcon(),
                                                           to.getClassName(),
                                                           from.getNumber()));
    myListModel.set(index + direction, new MIDPSettings.MIDletProperty(from.getName(),
                                                                       from.getIcon(),
                                                                       from.getClassName(),
                                                                       to.getNumber()));
    myMIDletList.removeSelectionInterval(index, index);
    myMIDletList.addSelectionInterval(index + direction, index + direction);
  }

  @Override
  public void reset() {
    super.reset();
    myMIDletName.setText(mySettings.getSettings().get(MIDPApplicationType.MIDLET_NAME));
    myMIDletJarUrl.setText(mySettings.getSettings().get(MIDPApplicationType.MIDLET_JAR_URL));
    myMIDletVersion.setText(mySettings.getSettings().get(MIDPApplicationType.MIDLET_VERSION));
    myMIDletVendor.setText(mySettings.getSettings().get(MIDPApplicationType.MIDLET_VENDOR));

    myListModel.clear();
    SortedSet<String> midletNumbers = mySettings.getMIDlets();
    for (String key : midletNumbers) {
      final String value = mySettings.getSettings().get(key);
      if (value != null) {
        myListModel.addElement(new MIDPSettings.MIDletProperty(key, value));
      }
    }
    myMIDletList.setModel(myListModel);
    myModified = false;

    mySettings.setDefaultModified(false);
    myTempSettings.putAll(mySettings.getSettings());
    myTempUserOptions.addAll(mySettings.getUserDefinedOptions());
  }

  @Override
  public void disposeUIResources() {}

  @Override
  public void apply() throws ConfigurationException {
    if (myMIDletName.getText() == null || myMIDletName.getText().length() == 0) {
      throw new ConfigurationException(J2MEBundle.message("module.settings.suit.not.specified"));
    }

    if (myMIDletJarUrl.getText() == null || myMIDletJarUrl.getText().length() == 0) {
      throw new ConfigurationException(J2MEBundle.message("compiler.jar.file.not.specified"));
    }


    if (myMIDletVersion.getText() == null || myMIDletVersion.getText().length() == 0) {
      throw new ConfigurationException(J2MEBundle.message("module.settings.version.not.specified"));
    }

    if (myMIDletVendor.getText() == null || myMIDletVendor.getText().length() == 0) {
      throw new ConfigurationException(J2MEBundle.message("module.settings.vendor.not.specified."));
    }

    mySettings.getSettings().clear();
    for (final String key : myTempSettings.keySet()) {
      if (!mySettings.isMidletKey(key)) {
        mySettings.putSetting(key, myTempSettings.get(key));
      }
    }

    for (int i = 0; i < myListModel.size(); i++) {
      final MIDPSettings.MIDletProperty midlet = ((MIDPSettings.MIDletProperty)myListModel.getElementAt(i));
      mySettings.putSetting(midlet.getNumber(), midlet.getValueString());
    }

    mySettings.getUserDefinedOptions().clear();
    for (final UserDefinedOption myTempUserOption : myTempUserOptions) {
      mySettings.getUserDefinedOptions().add(myTempUserOption);
    }

    mySettings.putSetting(MIDPApplicationType.MIDLET_NAME, myMIDletName.getText());
    mySettings.putSetting(MIDPApplicationType.MIDLET_JAR_URL, myMIDletJarUrl.getText());

    mySettings.putSetting(MIDPApplicationType.MIDLET_VERSION, myMIDletVersion.getText());
    mySettings.putSetting(MIDPApplicationType.MIDLET_VENDOR, myMIDletVendor.getText());
    super.apply();
    myModified = false;
    //  mySettings.setDefaultModified(false);
  }


  @Override
  public boolean isModified() {
    return myModified;
  }


  private class MIDletOptionsPanel extends DialogWrapper {

    private final JPanel myMIDletOptionsPanel = new JPanel(new BorderLayout());
    private final JPanel myLabelPanel = new JPanel(new GridLayout(3, 1));
    private final JPanel myFieldsPanel = new JPanel(new GridLayout(3, 1));

    private final TextFieldWithBrowseButton myMIDletClass = new TextFieldWithBrowseButton();
    private final JLabel myClassLabel = new JLabel(J2MEBundle.message("module.settings.midlet.class"));

    private final TextFieldWithBrowseButton myMIDletIcon = new TextFieldWithBrowseButton();
    private final JLabel myIconLabel = new JLabel(J2MEBundle.message("module.settings.icon"));

    private final JTextField myMIDletName = new JTextField();
    private final JLabel myNameLabel = new JLabel(J2MEBundle.message("module.settings.midlet.name"));
    private MIDPSettings.MIDletProperty myMIDlet;
    private boolean myEditing = false;

    private final String relativeResources = J2MEModuleProperties.getInstance(myModule).getResourcePath();//todo specify relative file chooser

    MIDletOptionsPanel(Component parent,
                              MIDPSettings.MIDletProperty midlet,
                              boolean isEditing) {
      super(parent, true);
      setTitle(midlet.getNumber());
      myMIDlet = midlet;
      init();
      myEditing = isEditing;
      if (!myEditing) {
        setOKActionEnabled(false);
      }
    }

    @Override
    protected JComponent createCenterPanel() {
      myMIDletOptionsPanel.add(myLabelPanel, BorderLayout.WEST);
      myMIDletOptionsPanel.add(myFieldsPanel, BorderLayout.CENTER);

      myLabelPanel.add(myNameLabel);
      myMIDletName.setText(myMIDlet.getName());
      myFieldsPanel.add(myMIDletName);


      myLabelPanel.add(myIconLabel);
      if (relativeResources != null && myMIDlet.getIcon() != null) {
        myMIDletIcon.setText((relativeResources + myMIDlet.getIcon()).replace('/', File.separatorChar));
      }
      myMIDletIcon.addBrowseFolderListener(J2MEBundle.message("module.settings.choose.midlet.icon"),
                                           J2MEBundle.message("module.settings.choose.icon", myMIDletName.getText()),
                                           myProject,
                                           FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
      myFieldsPanel.add(myMIDletIcon);

      myLabelPanel.add(myClassLabel);
      myMIDletClass.setText(myMIDlet.getClassName());
      myFieldsPanel.add(myMIDletClass);

      myMIDletClass.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          J2MEClassBrowser j2MEClassBrowser = new J2MEClassBrowser(myModule);
          j2MEClassBrowser.show();
          j2MEClassBrowser.setField(myMIDletClass);
        }
      });

      myMIDletClass.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
          if (myMIDletName.getText() == null || myMIDletName.getText().length() == 0) {
            myMIDletName.setText(myMIDletClass.getText());
          }
          update();
        }
      });

      myMIDletName.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
          update();
        }
      });

      JPanel whole = new JPanel(new BorderLayout());
      whole.add(myMIDletOptionsPanel, BorderLayout.NORTH);
      return whole;
    }

    private void update() {
      if (myMIDletName.getText() == null || myMIDletName.getText().length() == 0 || myMIDletClass.getText() == null || myMIDletClass.getText().length() == 0) {
        setOKActionEnabled(false);
      }
      else {
        setOKActionEnabled(true);
      }
    }

    @Override
    protected void doOKAction() {
      final String iconPath = getRelativeIconPath(FileUtil.toSystemIndependentName(myMIDletIcon.getText()));
      if (iconPath == null) return;
      myMIDlet = new MIDPSettings.MIDletProperty(myMIDletName.getText(),
                                                 iconPath,
                                                 myMIDletClass.getText(),
                                                 myMIDlet.getNumber());
      // if (myMIDlet.getNumber(), myMIDlet.getValueString())) {
      if (myEditing) {
        myListModel.setElementAt(myMIDlet, myMIDletList.getSelectedIndex());
      }
      else {
        myListModel.addElement(myMIDlet);
      }
      //}
      myModified = true;
      super.doOKAction();
    }
  }


  public class AdditionalOptionsPanel extends DialogWrapper {
    private JPanel myCentrePanel;


    private JTextField myDeleteConfirmField;
    private JLabel myDeleteConfirm;

    private TextFieldWithBrowseButton myInstallNotifyURL;
    private JLabel myInstallNotify;

    private JTextField myDataSizeField;
    private JLabel myDataSize;

    private TextFieldWithBrowseButton myIconField;
    private JLabel myIcon;

    private JTextField myDescriptionField;
    private JLabel myDescription;

    private TextFieldWithBrowseButton myInfoURLField;
    private JLabel myInfoUrl;
    private JPanel myUserPanel;

    private UserKeysConfigurable myUserKeysConfigurable;


    public AdditionalOptionsPanel(Component parent) {
      super(parent, true);
      setTitle(J2MEBundle.message("module.settings.optional.midp.settings"));
      init();
    }

    @Override
    protected JComponent createCenterPanel() {
      myDataSize.setText(MIDPApplicationType.MIDLET_DATA_SIZE + ":");
      myDataSizeField.setText(myTempSettings.get(MIDPApplicationType.MIDLET_DATA_SIZE));

      myDeleteConfirm.setText(MIDPApplicationType.MIDLET_DELETE_CONFIRM + ":");
      myDeleteConfirmField.setText(myTempSettings.get(MIDPApplicationType.MIDLET_DELETE_CONFIRM));

      myDescription.setText(MIDPApplicationType.MIDLET_DESCRIPTION + ":");
      myDescriptionField.setText(myTempSettings.get(MIDPApplicationType.MIDLET_DESCRIPTION));

      myIcon.setText(MIDPApplicationType.MIDLET_ICON + ":");
      final String iconPath = myTempSettings.get(MIDPApplicationType.MIDLET_ICON);
      if (iconPath != null) {
        final String resourcePath = J2MEModuleProperties.getInstance(myModule).getResourcePath();
        myIconField.setText((resourcePath != null ? resourcePath : "") + File.separator + FileUtil.toSystemDependentName(iconPath));
      }
      myIconField.addBrowseFolderListener(J2MEBundle.message("module.settings.choose.icon.common"),
                                          J2MEBundle.message("module.settings.choose.icon", myMIDletName.getText()),
                                          myProject,
                                          FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());

      myInfoUrl.setText(MIDPApplicationType.MIDLET_INFO_URL + ":");
      myInfoURLField.setText(myTempSettings.get(MIDPApplicationType.MIDLET_INFO_URL));
      myInfoURLField.addBrowseFolderListener(J2MEBundle.message("build.settings.file.url.title"),
                                             J2MEBundle.message("module.settings.application.url"),
                                             myProject,
                                             FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());

      myInstallNotify.setText(MIDPApplicationType.MIDLET_INSTALL_NOTIFY + ":");
      myInstallNotifyURL.setText(myTempSettings.get(MIDPApplicationType.MIDLET_INSTALL_NOTIFY));
      myInstallNotifyURL.addBrowseFolderListener(J2MEBundle.message("build.settings.file.url.title"),
                                                 J2MEBundle.message("module.settings.install.notify.url"),
                                                 myProject,
                                                 FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
      myUserPanel.setLayout(new BorderLayout());
      myUserKeysConfigurable = new UserKeysConfigurable(myTempUserOptions);
      myUserPanel.add(myUserKeysConfigurable.getUserKeysPanel(), BorderLayout.CENTER);
      return myCentrePanel;
    }

    @Override
    protected void doOKAction() {
      myUserKeysConfigurable.getTable().stopEditing();
      myTempSettings.put(MIDPApplicationType.MIDLET_DATA_SIZE, myDataSizeField.getText());
      myTempSettings.put(MIDPApplicationType.MIDLET_DELETE_CONFIRM, myDeleteConfirmField.getText());
      myTempSettings.put(MIDPApplicationType.MIDLET_DESCRIPTION, myDescriptionField.getText());
      final String iconPath = getRelativeIconPath(FileUtil.toSystemIndependentName(myIconField.getText()));
      if (iconPath == null) return;
      myTempSettings.put(MIDPApplicationType.MIDLET_ICON, iconPath);
      myTempSettings.put(MIDPApplicationType.MIDLET_INFO_URL, myInfoURLField.getText());
      myTempSettings.put(MIDPApplicationType.MIDLET_INSTALL_NOTIFY, myInstallNotifyURL.getText());
      myTempUserOptions.clear();
      myTempUserOptions.addAll(myUserKeysConfigurable.getUserDefinedOptions().getItems());
      myModified = true;
      super.doOKAction();
    }
  }

  @Nullable
  private String getRelativeIconPath(@NotNull String iconPath) {
    String resourcePath = J2MEModuleProperties.getInstance(myModule).getResourcePath();
    if (resourcePath == null) {
      if (iconPath.length() != 0) {
        Messages.showErrorDialog(J2MEBundle.message("module.settings.unable.to.set.icon"), J2MEBundle.message("resource.directory.needed"));
        return null;
      }
    }
    else {
      resourcePath = FileUtil.toSystemIndependentName(resourcePath);
      if (!iconPath.startsWith(resourcePath)) {
        if (iconPath.length() != 0) {
          Messages.showErrorDialog(J2MEBundle.message("module.settings.incorrect.icon.path"),
                                   J2MEBundle.message("module.settings.correct.icon.path", resourcePath));
          return null;
        }
      }
      else {
        iconPath = iconPath.substring(resourcePath.length());
      }
    }
    return iconPath;
  }
}
