package com.jetbrains.bigdatatools.flink.util

import com.intellij.icons.AllIcons
import com.jetbrains.bigdatatools.common.table.renderers.AbstractIconRenderer

class FlinkStatusIconRenderer : AbstractIconRenderer(flinkStatusIcons, AllIcons.RunConfigurations.TestUnknown) {
  companion object {
    val flinkStatusIcons = mapOf(
      "INITIALIZING" to AllIcons.Toolbar.AddSlot,
      "CREATED" to AllIcons.Welcome.CreateNewProjectTab,
      "RUNNING" to AllIcons.RunConfigurations.TestState.Run_run,
      "FAILING" to AllIcons.Status.FailedInProgress,
      "FAILED" to AllIcons.RunConfigurations.TestError,
      "CANCELLING" to AllIcons.Actions.StopRefresh,
      "CANCELED" to AllIcons.RunConfigurations.TestIgnored,
      "FINISHED" to AllIcons.RunConfigurations.TestPassed,
      "RESTARTING" to AllIcons.Actions.Restart,
      "SUSPENDED" to AllIcons.RunConfigurations.ToolbarSkipped,
      "RECONCILING" to AllIcons.RunConfigurations.TestUnknown
    )
  }
}