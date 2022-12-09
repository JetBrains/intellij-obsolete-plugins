package com.intellij.dmserver.facet;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public abstract class DMFacetsSwitch<T> {

  @Nullable
  public T processModule(@NotNull Module module) {
    ArrayList<DMFacetBase<?>> dmFacets = new ArrayList<>();
    DMBundleFacet bundleFacet = DMBundleFacet.getInstance(module);
    if (bundleFacet != null) {
      dmFacets.add(bundleFacet);
    }
    DMCompositeFacet compositeFacet = DMCompositeFacet.getInstance(module);
    if (compositeFacet != null) {
      dmFacets.add(compositeFacet);
    }
    DMConfigFacet configFacet = DMConfigFacet.getInstance(module);
    if (configFacet != null) {
      dmFacets.add(configFacet);
    }
    if (dmFacets.size() != 1) {
      return null; // user error - exactly one DM facet is expected
    }
    if (bundleFacet != null) {
      return doProcessBundleFacet(bundleFacet);
    }
    else if (compositeFacet != null) {
      return doProcessCompositeFacet(compositeFacet);
    }
    else if (configFacet != null) {
      return doProcessConfigFacet(configFacet);
    }
    return null; // impossible
  }

  protected abstract T doProcessBundleFacet(DMBundleFacet bundleFacet);

  protected abstract T doProcessCompositeFacet(DMCompositeFacet compositeFacet);

  protected abstract T doProcessConfigFacet(DMConfigFacet configFacet);
}
