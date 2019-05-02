/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
