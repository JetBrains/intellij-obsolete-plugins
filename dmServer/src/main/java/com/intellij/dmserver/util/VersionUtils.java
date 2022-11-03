package com.intellij.dmserver.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * @author michael.golubev
 */
public final class VersionUtils {
  public static final VersionRange emptyRange =
    new VersionRange(VersionRange.LEFT_CLOSED, Version.emptyVersion, null, VersionRange.RIGHT_CLOSED);

  @NotNull
  public static Version loadVersion(@Nullable String version) {
    if (version == null) {
      return Version.emptyVersion;
    }
    try {
      return new Version(version);
    }
    catch (IllegalArgumentException ex) {
      return Version.emptyVersion;
    }
  }

  @NotNull
  public static VersionRange version2range(@Nullable Version version) {
    return new VersionRange(VersionRange.LEFT_CLOSED, version, version, VersionRange.RIGHT_CLOSED);
  }

  @NotNull
  public static VersionRange loadVersionRange(@Nullable String versionRange) {
    if (versionRange == null) {
      return emptyRange;
    }
    try {
      return new VersionRange(versionRange);
    }
    catch (IllegalArgumentException ex) {
      return emptyRange;
    }
  }

  @Nullable
  public static VersionRange parseVersionRange(@Nullable String versionRange) {
    if (versionRange == null) {
      return null;
    }
    try {
      return new VersionRange(versionRange);
    }
    catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
