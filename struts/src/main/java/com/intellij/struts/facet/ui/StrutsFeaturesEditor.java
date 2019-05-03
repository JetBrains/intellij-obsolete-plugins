/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.facet.ui;

import com.intellij.facet.Facet;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.libraries.FacetLibrariesValidator;
import com.intellij.facet.ui.libraries.LibraryInfo;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Ref;
import com.intellij.struts.facet.AddStrutsSupportUtil;
import com.intellij.struts.facet.StrutsFacet;
import com.intellij.struts.facet.StrutsSupportModel;
import com.intellij.ui.EnumComboBoxModel;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class StrutsFeaturesEditor extends FacetEditorTab {
  private static final Logger LOG = Logger.getInstance("#com.intellij.struts.facet.ui.StrutsFeaturesEditor");

  private JPanel myMainPanel;
  private JPanel myDescriptionPanel;
  private JCheckBox myTilesCheckBox;
  private JCheckBox myValidatorCheckBox;
  private JCheckBox myStrutsELCheckBox;
  private JComboBox myVersionComboBox;
  private JCheckBox myStrutsTaglibCheckBox;
  private JCheckBox myScriptingCheckBox;
  private JCheckBox myExtrasCheckBox;
  private JCheckBox myStrutsFacesCheckBox;

  private final Ref<Boolean> myTilesSupport = Ref.create(false);
  private final Ref<Boolean> myValidatorSupport = Ref.create(false);
  private final Ref<Boolean> myStrutsTaglibSupport = Ref.create(false);
  private final Ref<Boolean> myStrutsELSupport = Ref.create(false);
  private final Ref<Boolean> myScriptingSupport = Ref.create(false);
  private final Ref<Boolean> myExtrasSupport = Ref.create(false);
  private final Ref<Boolean> myStrutsFacesSupport = Ref.create(false);
  private StrutsVersion myVersion;
  private final FacetEditorContext myEditorContext;
  private final FacetLibrariesValidator myLibrariesValidator;
  private final List<BooleanConfigurableElement> myConfigurables = new ArrayList<>();
  private LibraryInfo[] myLastLibraryInfos = LibraryInfo.EMPTY_ARRAY;

  public StrutsFeaturesEditor(FacetEditorContext editorContext, final FacetLibrariesValidator librariesValidator) {
    myEditorContext = editorContext;
    myLibrariesValidator = librariesValidator;
    initCheckboxes();
    final Facet parentFacet = myEditorContext.getParentFacet();
    if (parentFacet != null) {
      init((WebFacet)parentFacet);
    }

    myVersionComboBox.setModel(new EnumComboBoxModel<>(StrutsVersion.class));
    myVersionComboBox.setEnabled(myVersion == null);
    if (myVersion == null) {
      myVersion = StrutsVersion.Struts1_3_8;
    }
    myVersionComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        selectVersion(getSelectedVersion());
      }
    });

    myStrutsTaglibCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        updateStrutsELCheckbox();
      }
    });


    myDescriptionPanel.setLayout(new BorderLayout());
    myDescriptionPanel.add(createDescriptionPanel(), BorderLayout.CENTER);

    addCheckboxesListeners();
  }

  private void updateStrutsELCheckbox() {
    if (!myStrutsTaglibCheckBox.isSelected()) {
      myStrutsELCheckBox.setSelected(false);
    }
    myStrutsELCheckBox.setEnabled(myStrutsTaglibCheckBox.isSelected());
  }

  private StrutsVersion getSelectedVersion() {
    return (StrutsVersion)myVersionComboBox.getModel().getSelectedItem();
  }

  private void initCheckboxes() {
    myConfigurables.add(new BooleanConfigurableElement(myTilesCheckBox, myTilesSupport));
    myConfigurables.add(new BooleanConfigurableElement(myExtrasCheckBox, myExtrasSupport));
    myConfigurables.add(new BooleanConfigurableElement(myScriptingCheckBox, myScriptingSupport));
    myConfigurables.add(new BooleanConfigurableElement(myStrutsELCheckBox, myStrutsELSupport));
    myConfigurables.add(new BooleanConfigurableElement(myStrutsFacesCheckBox, myStrutsFacesSupport));
    myConfigurables.add(new BooleanConfigurableElement(myStrutsTaglibCheckBox, myStrutsTaglibSupport));
    myConfigurables.add(new BooleanConfigurableElement(myValidatorCheckBox, myValidatorSupport));
  }

  private void addCheckboxesListeners() {
    for (final BooleanConfigurableElement configurable : myConfigurables) {
      configurable.getCheckBox().addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
          updateRequiredLibraries();
        }
      });
    }
  }

  private void updateRequiredLibraries() {
    final LibraryInfo[] libraries = getRequiredLibraries();
    if (!Arrays.equals(libraries, myLastLibraryInfos)) {
      myLibrariesValidator.setRequiredLibraries(libraries);
      myLastLibraryInfos = libraries;
    }
  }

  private void selectVersion(final StrutsVersion strutsVersion) {
    switch (strutsVersion) {
      case Struts1_2_9:
        myStrutsTaglibCheckBox.setEnabled(false);
        myStrutsTaglibCheckBox.setSelected(true);

        myStrutsFacesCheckBox.setEnabled(false);
        myStrutsFacesCheckBox.setSelected(false);

        myScriptingCheckBox.setEnabled(false);
        myScriptingCheckBox.setSelected(false);

        myExtrasCheckBox.setEnabled(false);
        myExtrasCheckBox.setSelected(false);
        break;
      case Struts1_3_8:
        myExtrasCheckBox.setEnabled(!myExtrasSupport.get());
        myExtrasCheckBox.setSelected(myExtrasSupport.get());

        myScriptingCheckBox.setEnabled(!myScriptingSupport.get());
        myScriptingCheckBox.setSelected(myScriptingSupport.get());

        myStrutsFacesCheckBox.setEnabled(!myStrutsFacesSupport.get());
        myStrutsFacesCheckBox.setSelected(myStrutsFacesSupport.get());

        myStrutsTaglibCheckBox.setEnabled(!myStrutsTaglibSupport.get() || myEditorContext.isNewFacet());
        myStrutsTaglibCheckBox.setSelected(myStrutsTaglibSupport.get() || myEditorContext.isNewFacet());

        break;
    }
  }

  @Override
  public void reset() {
    for (BooleanConfigurableElement configurable : myConfigurables) {
      configurable.reset();
    }
    myStrutsTaglibCheckBox.setSelected(myStrutsTaglibSupport.get() || myEditorContext.isNewFacet());
    updateStrutsELCheckbox();
    myVersionComboBox.setSelectedItem(myVersion);
    updateRequiredLibraries();
  }

  @Override
  public boolean isModified() {
    for (BooleanConfigurableElement configurable : myConfigurables) {
      if (configurable.isModified()) {
        return true;
      }
    }
    return myVersionComboBox.getSelectedItem() != myVersion ||
           myLibrariesValidator.isLibrariesAdded();
  }

  @Override
  @NotNull
  public JComponent createComponent() {
    return myMainPanel;
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "Struts Features";
  }

  @Override
  public void apply() {
    for (BooleanConfigurableElement configurable : myConfigurables) {
      configurable.apply();
    }
    myVersion = getSelectedVersion();

  }

  @Override
  public void onFacetInitialized(@NotNull final Facet facet) {
    myLibrariesValidator.onFacetInitialized(facet);
    final StrutsFacet strutsFacet = (StrutsFacet)facet;
    final WebFacet webFacet = strutsFacet.getWebFacet();
    assert webFacet != null;
    AddStrutsSupportUtil.addSupportInWriteCommandAction(webFacet, myValidatorSupport.get(), myTilesSupport.get(), myVersion);
  }

  @NotNull
  private LibraryInfo[] getRequiredLibraries() {
    LibraryInfo[] libs = myVersion.getJars();
    if (myStrutsTaglibCheckBox.isSelected() && myVersion.getStrutsTaglib() != null) {
      libs = ArrayUtil.append(libs, myVersion.getStrutsTaglib());
    }
    if ((myTilesCheckBox.isSelected() || myStrutsFacesCheckBox.isSelected()) && myVersion.getTiles() != null) {
      libs = ArrayUtil.append(libs, myVersion.getTiles());
    }
    if (myStrutsFacesCheckBox.isSelected()) {
      libs = ArrayUtil.append(libs, myVersion.getStrutsFaces());
    }
    if (myScriptingCheckBox.isSelected()) {
      libs = ArrayUtil.mergeArrays(libs, myVersion.getScripting());
    }
    if (myExtrasCheckBox.isSelected()) {
      libs = ArrayUtil.append(libs, myVersion.getExtras());
    }
    if (myStrutsELCheckBox.isSelected()) {
      libs = ArrayUtil.mergeArrays(libs, myVersion.getStrutsEl());
    }
    return libs;
  }

  private void init(@NotNull final WebFacet webFacet) {
    DumbService.getInstance(webFacet.getModule().getProject()).runWhenSmart(() -> {
      StrutsSupportModel model = StrutsSupportModel.checkStrutsSupport(webFacet);
      if (model.isStrutsLib()) {
        myVersion = model.isStruts13() ? StrutsVersion.Struts1_3_8 : StrutsVersion.Struts1_2_9;
      }
      myTilesSupport.set(model.isTiles());
      myValidatorSupport.set(model.isValidation());
      myStrutsELSupport.set(model.isStrutsEl());
      myStrutsTaglibSupport.set(model.isStrutsTaglib());
      myStrutsFacesSupport.set(model.isStrutsFaces());
      myScriptingSupport.set(model.isScripting());
      myExtrasSupport.set(model.isExtras());
    });
  }

  public static JPanel createDescriptionPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JLabel("Struts is a Java web-application development framework."), BorderLayout.NORTH);
    final HyperlinkLabel hyperlinkLabel = new HyperlinkLabel("More about Struts");
    hyperlinkLabel.setHyperlinkTarget("http://struts.apache.org/");
    panel.add(hyperlinkLabel, BorderLayout.SOUTH);
    return panel;
  }

  @Override
  public String getHelpTopic() {
    return "reference.settings.project.modules.struts.facet.tab.features";
  }

}
