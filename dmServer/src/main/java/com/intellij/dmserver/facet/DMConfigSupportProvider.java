package com.intellij.dmserver.facet;

import com.intellij.ide.util.frameworkSupport.FrameworkVersion;
import com.intellij.openapi.roots.ModifiableRootModel;

public class DMConfigSupportProvider extends DMFacetSupportProviderBase<DMConfigFacet, DMConfigFacetConfiguration> {

  public DMConfigSupportProvider() {
    super(DMConfigFacet.ID);
  }

  @Override
  protected void setupConfiguration(DMConfigFacet facet, ModifiableRootModel rootModel, FrameworkVersion version) {
    //
  }

  @Override
  public String getUnderlyingFrameworkId() {
    return null;
  }
}
