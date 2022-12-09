package com.intellij.dmserver.util;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;

public final class PathUtils {

  private PathUtils() {
  }

  public static String concatPaths(@NotNull String prefix, @NotNull String suffix) {
    return FileUtil.toSystemIndependentName(prefix.trim() + "/" + suffix.trim());
  }
}
