// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util.version;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Version extends Comparable<Version> {

  Version LATEST_2x = new Version() {

    @Override
    public int compareTo(@NotNull Version other) {
      if (other == this) return 0;
      // if other version is less than 3.0, then this version is greater than other
      return other.compareTo(GRAILS_3_0) < 0 ? 1 : -1;
    }

    @Override
    public String toString() {
      return "Latest 2.x";
    }
  };

  Version GRAILS_1_1 = new VersionImpl("1.1");
  Version GRAILS_1_2 = new VersionImpl("1.2");
  Version GRAILS_1_3_4 = new VersionImpl("1.3.4");
  Version GRAILS_2_0 = new VersionImpl("2.0");
  Version GRAILS_2_3_0 = new VersionImpl("2.3.0");
  Version GRAILS_2_3_5 = new VersionImpl("2.3.5");
  Version GRAILS_2_3_10 = new VersionImpl("2.3.10");
  Version GRAILS_2_4_0 = new VersionImpl("2.4.0");
  Version GRAILS_3_0 = new VersionImpl("3.0");
  Version GRAILS_3_1_5 = new VersionImpl("3.1.5");
  Version GRAILS_4_0 = new VersionImpl("4.0.0");
  Version GRAILS_6_0 = new VersionImpl("6.0.0");
  Range<Version> LESS_THAN_3 = new Range<Version>().setEnd(GRAILS_3_0).setEndInclusive(false);
  Range<Version> AT_LEAST_3 = new Range<Version>().setStart(GRAILS_3_0).setStartInclusive(true);
  Range<Version> AT_LEAST_4 = new Range<Version>().setStart(GRAILS_4_0).setStartInclusive(true);


  @Override
  int compareTo(@NotNull Version o);

  default boolean equalsToString(@Nullable String other) {
    return other != null && equals(new VersionImpl(other));
  }

  default int compareToString(@NotNull String other) {
    return compareTo(new VersionImpl(other));
  }

  default boolean isAtLeast(@NotNull Version other) {
    return compareTo(other) >= 0;
  }

  default boolean isAtLeast(@NotNull String other) {
    return compareToString(other) >= 0;
  }

  default boolean isLessThan(@NotNull Version other) {
    return compareTo(other) < 0;
  }

  default boolean isLessThan(@NotNull String other) {
    return compareToString(other) < 0;
  }
}
