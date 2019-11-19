package com.jetbrains.plugins.compass;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CompassConfig {
  public static final CompassConfig EMPTY_COMPASS_CONFIG = new CompassConfig(Collections.emptyList());
  
  @NotNull
  private List<String> myImportPaths;

  public CompassConfig(@NotNull List<String> importPaths) {
    myImportPaths = importPaths;
  }

  @NotNull
  public List<String> getImportPaths() {
    return myImportPaths;
  }

  @SuppressWarnings("UnusedDeclaration")
  //needed for serialization
  public CompassConfig() {}

  @SuppressWarnings("UnusedDeclaration")
  //needed for serialization
  public void setImportPaths(@NotNull List<String> importPaths) {
    myImportPaths = importPaths;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CompassConfig that = (CompassConfig)o;

    if (!myImportPaths.equals(that.myImportPaths)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myImportPaths.hashCode();
  }
}
