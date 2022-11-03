package com.intellij.dmserver.facet;

import com.intellij.facet.ui.FacetEditorContext;
import org.jetbrains.annotations.Nls;

public class DMCompositeFacetEditor extends DMFacetEditorBase<DMCompositeFacet, DMCompositeFacetConfiguration> {

  public DMCompositeFacetEditor(FacetEditorContext facetEditorContext, DMCompositeFacetConfiguration facetConfiguration) {
    super(facetEditorContext, facetConfiguration, new DMModuleCompositeFacetSettingsPanel());
  }

  @Override
  @Nls
  public String getDisplayName() {
    return DMCompositeFacetType.getDisplayName();
  }

  @Override
  protected DMCompositeFacetConfiguration createFacetConfiguration() {
    return new DMCompositeFacetConfiguration();
  }
}
