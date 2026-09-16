package com.intellij.bigdatatools.databricks.sync

import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.icons.AllIcons
import org.jetbrains.annotations.Nls
import javax.swing.Icon

enum class SyncStatus(val icon: Icon, @Nls val title: String) {
  IN_PROGRESS(AllIcons.RunConfigurations.TestState.Run_run, DatabricksBundle.message("workspace.sync.status.in.progress")),
  WATCHING_FOR_CHANGES(AllIcons.RunConfigurations.TestPassed, DatabricksBundle.message("workspace.sync.status.watching.for.changes")),
  STOPPED(AllIcons.RunConfigurations.TestIgnored, DatabricksBundle.message("workspace.sync.status.stopped")),
  ERROR(AllIcons.RunConfigurations.TestFailed, DatabricksBundle.message("workspace.sync.status.error"));

  fun isRunning() = this in setOf(IN_PROGRESS, WATCHING_FOR_CHANGES)
}