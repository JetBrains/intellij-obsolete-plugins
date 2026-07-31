package com.intellij.gwt.run;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public abstract class GwtDevModeServer {
  private final String myName;
  private final String myId;

  protected GwtDevModeServer(@NotNull @NonNls String id, @NotNull @NlsSafe String name) {
    myName = name;
    myId = id;
  }

  public @NlsSafe String getName() {
    return myName;
  }

  public final String getId() {
    return myId;
  }

  public @Nullable Icon getIcon() {
    return null;
  }

  public abstract void patchParameters(@NotNull JavaParameters parameters, String originalOutputDir, @NotNull GwtFacet gwtFacet);

  public @NotNull String patchWarDirectoryPath(@NotNull String warDirectoryPath) {
    return warDirectoryPath;
  }
}
