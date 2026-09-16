package com.jetbrains.spark.monitoring.ui.table.renderers

import com.intellij.icons.AllIcons
import com.jetbrains.bigdatatools.common.table.renderers.AbstractIconRenderer
import com.jetbrains.spark.monitoring.data.TaskStatus

class SparkTaskStatusIconRenderer : AbstractIconRenderer(defaultIcons, AllIcons.RunConfigurations.TestUnknown) {
  companion object {
    val defaultIcons = TaskStatus.entries.associateWith {
      when (it) {
        TaskStatus.GET_RESULT -> AllIcons.RunConfigurations.TestPassed
        TaskStatus.RUNNING -> AllIcons.RunConfigurations.TestState.Run_run
        TaskStatus.FAILED -> AllIcons.RunConfigurations.TestFailed
        TaskStatus.KILLED -> AllIcons.RunConfigurations.TestTerminated
        TaskStatus.SUCCESS -> AllIcons.RunConfigurations.TestPassed
        TaskStatus.UNKNOWN -> AllIcons.RunConfigurations.TestUnknown
      }
    }.map { it.key.name to it.value }.toMap()
  }
}