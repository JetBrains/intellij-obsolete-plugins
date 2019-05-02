package com.intellij.struts.facet.ui;

import com.intellij.facet.ui.FacetEditor;
import com.intellij.facet.ui.FacetEditorsFactory;
import com.intellij.facet.ui.MultipleFacetEditorHelper;
import com.intellij.facet.ui.MultipleFacetSettingsEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author nik
 */
public class MultipleStrutsFacetEditor extends MultipleFacetSettingsEditor {
  private final MultipleFacetEditorHelper myHelper;
  private final StrutsFacetCommonSettingsPanel mySettingsPanel;

  public MultipleStrutsFacetEditor(@NotNull FacetEditor[] editors) {
    mySettingsPanel = new StrutsFacetCommonSettingsPanel();

    myHelper = FacetEditorsFactory.getInstance().createMultipleFacetEditorHelper();
    myHelper.bind(mySettingsPanel.getDisablePropertyKeysValidationCheckBox(), editors, facetEditor -> facetEditor.getEditorTab(StrutsValidationEditor.class).getSettingsPanel().getDisablePropertyKeysValidationCheckBox());
  }

  @Override
  public JComponent createComponent() {
    return mySettingsPanel.getMainPanel();
  }

  @Override
  public void disposeUIResources() {
    myHelper.unbind();
  }
}
