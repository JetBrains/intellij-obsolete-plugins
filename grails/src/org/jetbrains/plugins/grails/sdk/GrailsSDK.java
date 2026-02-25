// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.sdk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.version.Version;

public class GrailsSDK {

  private final @NotNull String myPath;
  private final @NotNull Version myVersion;

  public GrailsSDK(@NotNull String path, @NotNull Version version) {
    myPath = path;
    myVersion = version;
  }

  public @NotNull Version getVersion() {
    return myVersion;
  }

  public @NotNull String getPath() {
    return myPath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GrailsSDK sdk = (GrailsSDK)o;

    if (!myVersion.equals(sdk.myVersion)) return false;
    if (!myPath.equals(sdk.myPath)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myVersion.hashCode();
    result = 31 * result + (myPath.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return String.format("GrailsSDK {myPath='%s', myVersion=%s}", myPath, myVersion);
  }
}
