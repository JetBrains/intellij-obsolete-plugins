// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.gspIndex;

import java.util.Arrays;

public class GspIncludeInfo {
  
  private final int myOffset;

  private final String[] myNamedArguments;

  public GspIncludeInfo(int offset, String[] namedArguments) {
    this.myOffset = offset;
    this.myNamedArguments = namedArguments;
  }

  public int getOffset() {
    return myOffset;
  }

  public String[] getNamedArguments() {
    return myNamedArguments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GspIncludeInfo info = (GspIncludeInfo)o;

    if (myOffset != info.myOffset) return false;
    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    if (!Arrays.equals(myNamedArguments, info.myNamedArguments)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myOffset;
    result = 31 * result + Arrays.hashCode(myNamedArguments);
    return result;
  }
}
