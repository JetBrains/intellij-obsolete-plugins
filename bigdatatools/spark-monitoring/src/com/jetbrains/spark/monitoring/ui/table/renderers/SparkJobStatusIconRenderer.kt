package com.jetbrains.spark.monitoring.ui.table.renderers

import com.intellij.icons.AllIcons
import com.jetbrains.bigdatatools.common.table.renderers.AbstractIconRenderer
import com.jetbrains.spark.monitoring.data.JobExecutionStatus

class SparkJobStatusIconRenderer : AbstractIconRenderer(defaultIcons, AllIcons.RunConfigurations.TestUnknown) {
  companion object {
    val defaultIcons = JobExecutionStatus.entries.associateWith {
      when (it) {
        JobExecutionStatus.RUNNING -> AllIcons.RunConfigurations.TestState.Run_run
        JobExecutionStatus.SUCCEEDED -> AllIcons.RunConfigurations.TestPassed
        JobExecutionStatus.FAILED -> AllIcons.RunConfigurations.TestFailed
        JobExecutionStatus.UNKNOWN -> AllIcons.RunConfigurations.TestUnknown
      }
    }.map { it.key.name to it.value }.toMap()
  }
}