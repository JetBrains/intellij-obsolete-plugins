package com.intellij.dmserver.facet;

import com.intellij.facet.ui.FacetEditorContext;
import org.jetbrains.annotations.Nls;

public class DMConfigFacetEditor extends DMFacetEditorBase<DMConfigFacet, DMConfigFacetConfiguration> {

  public DMConfigFacetEditor(FacetEditorContext facetEditorContext, DMConfigFacetConfiguration facetConfiguration) {
    super(facetEditorContext, facetConfiguration, new DMModuleConfigFacetSettingsPanel());
  }

  @Override
  @Nls
  public String getDisplayName() {
    return DMConfigFacetType.getDisplayName();
  }

  @Override
  protected DMConfigFacetConfiguration createFacetConfiguration() {
    return new DMConfigFacetConfiguration();
  }
}
