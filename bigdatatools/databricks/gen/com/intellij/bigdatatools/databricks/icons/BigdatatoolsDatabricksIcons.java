package com.intellij.bigdatatools.databricks.icons;

import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * NOTE THIS FILE IS AUTO-GENERATED
 * DO NOT EDIT IT BY HAND, run "Generate icon classes" configuration instead
 */
public final class BigdatatoolsDatabricksIcons {
  private static @NotNull Icon load(@NotNull String path, int cacheKey, int flags) {
    return IconManager.getInstance().loadRasterizedIcon(path, BigdatatoolsDatabricksIcons.class.getClassLoader(), cacheKey, flags);
  }
  private static @NotNull Icon load(@NotNull String expUIPath, @NotNull String path, int cacheKey, int flags) {
    return IconManager.getInstance().loadRasterizedIcon(path, expUIPath, BigdatatoolsDatabricksIcons.class.getClassLoader(), cacheKey, flags);
  }
  /** 16x16 */ public static final @NotNull Icon Cluster = load("icons/cluster.svg", 2051124640, 2);
  /** 16x16 */ public static final @NotNull Icon Databricks = load("icons/databricks.svg", 1561490312, 0);
  /** 16x16 */ public static final @NotNull Icon DatabricksDBFS = load("icons/databricksDBFS.svg", -1610760997, 0);
  /** 13x13 */ public static final @NotNull Icon DatabricksToolWindow = load("icons/expui/databricksToolWindow.svg", "icons/databricksToolWindow.svg", 37194744, 2);
  /** 16x16 */ public static final @NotNull Icon DatabricksWorkspace = load("icons/databricksWorkspace.svg", -1727988998, 0);
  /** 16x16 */ public static final @NotNull Icon Empty = load("icons/empty.svg", 522771004, 0);
  /** 16x16 */ public static final @NotNull Icon Error = load("icons/error.svg", 1206479358, 0);
  /** 16x16 */ public static final @NotNull Icon Maven = load("icons/maven.svg", -1673024365, 2);
  /** 13x13 */ public static final @NotNull Icon RPackages = load("icons/RPackages.svg", -1018126418, 0);
  /** 16x16 */ public static final @NotNull Icon Running = load("icons/running.svg", -1687278597, 0);

  public static final class Status {
    /** 16x16 */ public static final @NotNull Icon StatusError = load("icons/expui/status/statusError.svg", "icons/status/statusError.svg", 2026052781, 0);
    /** 16x16 */ public static final @NotNull Icon StatusFailed = load("icons/expui/status/statusFailed.svg", "icons/status/statusFailed.svg", 1825371381, 0);
    /** 16x16 */ public static final @NotNull Icon StatusNotRun = load("icons/expui/status/statusNotRun.svg", "icons/status/statusNotRun.svg", 601706378, 0);
    /** 16x16 */ public static final @NotNull Icon StatusPassed = load("icons/expui/status/statusPassed.svg", "icons/status/statusPassed.svg", -1718343567, 0);
    /** 16x16 */ public static final @NotNull Icon StatusPassedIgnored = load("icons/expui/status/statusPassedIgnored.svg", "icons/status/statusPassedIgnored.svg", -100850140, 0);
    /** 16x16 */ public static final @NotNull Icon StatusSkipped = load("icons/expui/status/statusSkipped.svg", "icons/status/statusSkipped.svg", -1363803905, 0);
  }

  /** 16x16 */ public static final @NotNull Icon Warning = load("icons/warning.svg", -451526102, 2);
}
