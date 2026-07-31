package com.intellij.gwt.run;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.gwt.facet.GwtFacet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class DefaultDevModeServer extends GwtDevModeServer {
  public static final @NonNls String SERVER_ID = "default";

  public DefaultDevModeServer() {
    super(SERVER_ID, "Default");
  }

  @Override
  public void patchParameters(@NotNull JavaParameters parameters, String originalOutputDir, @NotNull GwtFacet gwtFacet) {
  }
}
