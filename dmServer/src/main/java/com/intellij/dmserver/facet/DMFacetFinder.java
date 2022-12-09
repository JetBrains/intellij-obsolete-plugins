package com.intellij.dmserver.facet;

public final class DMFacetFinder extends DMFacetsSwitch<DMFacetBase> {

  private static final DMFacetFinder ourInstance = new DMFacetFinder();

  public static DMFacetFinder getInstance() {
    return ourInstance;
  }

  private DMFacetFinder() {

  }

  @Override
  protected DMFacetBase doProcessBundleFacet(DMBundleFacet bundleFacet) {
    return bundleFacet;
  }

  @Override
  protected DMFacetBase doProcessCompositeFacet(DMCompositeFacet compositeFacet) {
    return compositeFacet;
  }

  @Override
  protected DMFacetBase doProcessConfigFacet(DMConfigFacet configFacet) {
    return configFacet;
  }
}
