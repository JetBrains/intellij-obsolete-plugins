package com.intellij.gwt.maven;

import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.sdk.impl.GwtSdkBase;
import com.intellij.gwt.sdk.impl.GwtVersionDetector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtMavenSdkPaths;

public class GwtMavenSdk extends GwtSdkBase {
  private final String myVersion;

  public GwtMavenSdk(String homeDirectoryPath, final String version) {
    super(new GwtMavenSdkPaths(homeDirectoryPath, version));
    myVersion = version;
  }

  @Override
  protected @NotNull GwtVersion detectVersion() {
    return GwtVersionDetector.getGwtVersionFromString(myVersion);
  }

  public @NotNull GwtMavenSdkPaths getGwtSdkPaths() {
    return (GwtMavenSdkPaths)myPaths;
  }

  @Override
  public boolean isValid() {
    return true;
  }
}
