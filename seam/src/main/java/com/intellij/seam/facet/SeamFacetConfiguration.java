package com.intellij.seam.facet;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetEditorsFactory;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.facet.ui.libraries.FacetLibrariesValidator;
import com.intellij.facet.ui.libraries.FacetLibrariesValidatorDescription;
import com.intellij.facet.ui.libraries.LibraryInfo;

public class SeamFacetConfiguration implements FacetConfiguration {

  @Override
  public FacetEditorTab[] createEditorTabs(final FacetEditorContext editorContext, final FacetValidatorsManager validatorsManager) {
    final FacetLibrariesValidator validator = FacetEditorsFactory.getInstance()
      .createLibrariesValidator(LibraryInfo.EMPTY_ARRAY, new FacetLibrariesValidatorDescription("seam"), editorContext, validatorsManager);
    validatorsManager.registerValidator(validator);

    return new FacetEditorTab[]{new SeamFeaturesEditor(editorContext, validator)};
  }
}

