package com.intellij.gwt.facet;

import com.intellij.facet.ui.FacetEditor;
import com.intellij.facet.ui.FacetEditorsFactory;
import com.intellij.facet.ui.MultipleFacetEditorHelper;
import com.intellij.facet.ui.MultipleFacetSettingsEditor;
import com.intellij.openapi.project.Project;

import javax.swing.JComponent;

public class MultipleGwtFacetSettingsEditor extends MultipleFacetSettingsEditor {
  private final GwtFacetCommonSettingsPanel myCommonSettingsPanel;
  private final MultipleFacetEditorHelper myHelper;

  public MultipleGwtFacetSettingsEditor(final Project project, final FacetEditor[] editors) {
    myCommonSettingsPanel = new GwtFacetCommonSettingsPanel(project);

    myHelper = FacetEditorsFactory.getInstance().createMultipleFacetEditorHelper();
    myHelper.bind(myCommonSettingsPanel.getCompilerHeapSizeField(), editors, facetEditor -> facetEditor.getEditorTab(GwtFacetEditor.class).getCompilerHeapSizeField());
    myHelper.bind(myCommonSettingsPanel.getOutputStyleComboBox(), editors, facetEditor -> facetEditor.getEditorTab(GwtFacetEditor.class).getOutputStyleBox());
    myHelper.bind(myCommonSettingsPanel.getSdkPathEditor().getPathTextField(), editors, facetEditor -> facetEditor.getEditorTab(GwtFacetEditor.class).getGwtPathEditor().getPathTextField());
  }

  @Override
  public JComponent createComponent() {
    return myCommonSettingsPanel.getMainPanel();
  }

  @Override
  public void disposeUIResources() {
    myHelper.unbind();
  }
}
