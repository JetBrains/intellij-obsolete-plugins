package com.intellij.dmserver.facet;

import com.intellij.facet.ui.FacetEditor;
import com.intellij.facet.ui.MultipleFacetSettingsEditor;
import com.intellij.openapi.project.Project;

import javax.swing.*;

public class DMMultipleFacetSettingsEditor extends MultipleFacetSettingsEditor {
  private final DMProjectFacetSettingsPanel myProjectSettingsPanel;

  public DMMultipleFacetSettingsEditor(Project project, FacetEditor[] editors) {
    myProjectSettingsPanel = new DMProjectFacetSettingsPanel();
    myProjectSettingsPanel.init(project);
  }

  @Override
  public JComponent createComponent() {
    return myProjectSettingsPanel.getMainPanel();
  }
}
