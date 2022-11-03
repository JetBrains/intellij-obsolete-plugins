package com.intellij.dmserver.facet;

import com.intellij.ide.util.frameworkSupport.FrameworkVersion;
import com.intellij.openapi.roots.ModifiableRootModel;

public class DMCompositeSupportProvider extends DMFacetSupportProviderBase<DMCompositeFacet, DMCompositeFacetConfiguration> {

  public DMCompositeSupportProvider() {
    super(DMCompositeFacet.ID);
  }

  @Override
  protected void setupConfiguration(DMCompositeFacet facet, ModifiableRootModel rootModel, FrameworkVersion version) {
    //
  }

  @Override
  public String getUnderlyingFrameworkId() {
    return null;
  }
}


