package com.intellij.seam.constants;

import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.seam.facet.SeamFacet;

public final class SeamDataKeys {
  public static final DataKey<SeamFacet> SEAM_FACET = DataKey.create("SEAM_FACET");

  private SeamDataKeys() {
  }
}
