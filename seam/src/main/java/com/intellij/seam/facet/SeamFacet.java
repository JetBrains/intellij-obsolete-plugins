package com.intellij.seam.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.Nullable;

public class SeamFacet extends Facet<SeamFacetConfiguration> {
  public final static FacetTypeId<SeamFacet> FACET_TYPE_ID = new FacetTypeId<>("seam");

  public SeamFacet(final FacetType facetType, final Module module, final String name, final SeamFacetConfiguration configuration, final Facet underlyingFacet) {
    super(facetType, module, name, configuration, underlyingFacet);
  }

  @Nullable
  public static SeamFacet getInstance(Module module) {
    return FacetManager.getInstance(module).getFacetByType(FACET_TYPE_ID);
  }
}
