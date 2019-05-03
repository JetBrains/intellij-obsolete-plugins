/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.facet.ui;

import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.struts.facet.StrutsValidationConfiguration;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public class StrutsValidationEditor extends FacetEditorTab {
  private final StrutsFacetDefaultSettingsEditor myDefaultSettingsEditor;

  public StrutsValidationEditor(StrutsValidationConfiguration configuration) {
    myDefaultSettingsEditor = new StrutsFacetDefaultSettingsEditor(configuration);
  }

  @Override
  @NotNull
  public JComponent createComponent() {
    return myDefaultSettingsEditor.createComponent();
  }

  public StrutsFacetCommonSettingsPanel getSettingsPanel() {
    return myDefaultSettingsEditor.getSettingsPanel();
  }

  @Override
  public boolean isModified() {
    return myDefaultSettingsEditor.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    myDefaultSettingsEditor.apply();
  }

  @Override
  public void reset() {
    myDefaultSettingsEditor.reset();
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "Validation";
  }

  @Override
  public String getHelpTopic() {
    return "reference.settings.project.modules.struts.facet.tab.validation";
  }
}
