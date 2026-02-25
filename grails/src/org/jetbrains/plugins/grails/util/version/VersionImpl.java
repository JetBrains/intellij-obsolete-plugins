// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util.version;

import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;

public class VersionImpl implements Version {

  private final @NotNull String myVersionString;

  public VersionImpl(@NotNull String string) {
    myVersionString = string;
  }

  @Override
  public int compareTo(@NotNull Version o) {
    if (o instanceof VersionImpl) {
      return this.equals(o) ? 0 : VersionComparatorUtil.compare(myVersionString, ((VersionImpl)o).myVersionString);
    }
    else {
      return -o.compareTo(this);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VersionImpl version = (VersionImpl)o;

    if (!myVersionString.equals(version.myVersionString)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myVersionString.hashCode();
  }

  @Override
  public String toString() {
    return myVersionString;
  }
}
