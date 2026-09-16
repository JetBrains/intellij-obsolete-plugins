package com.intellij.bigdatatools.sparkMonitoring.icons;

import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * NOTE THIS FILE IS AUTO-GENERATED
 * DO NOT EDIT IT BY HAND, run "Generate icon classes" configuration instead
 */
public final class BigdatatoolsSparkMonitoringIcons {
  private static @NotNull Icon load(@NotNull String path, int cacheKey, int flags) {
    return IconManager.getInstance().loadRasterizedIcon(path, BigdatatoolsSparkMonitoringIcons.class.getClassLoader(), cacheKey, flags);
  }
  private static @NotNull Icon load(@NotNull String expUIPath, @NotNull String path, int cacheKey, int flags) {
    return IconManager.getInstance().loadRasterizedIcon(path, expUIPath, BigdatatoolsSparkMonitoringIcons.class.getClassLoader(), cacheKey, flags);
  }
  /** 16x16 */ public static final @NotNull Icon Spark = load("icons/spark.svg", -1654521650, 0);
  /** 13x13 */ public static final @NotNull Icon SparkToolWindow = load("icons/expui/spark.svg", "icons/sparkToolWindow.svg", 681852250, 2);

  public static final class States {
    /** 16x16 */ public static final @NotNull Icon Pending = load("icons/states/pending.svg", 1169716783, 0);
  }

  /** 16x16 */ public static final @NotNull Icon TasksTable = load("icons/tasksTable.svg", 1792207046, 2);
}
