package com.jetbrains.spark.monitoring.ui.table.renderers

import com.intellij.icons.AllIcons
import com.intellij.ui.AnimatedIcon
import com.jetbrains.bigdatatools.common.table.renderers.AbstractIconRenderer
import com.jetbrains.spark.monitoring.data.ApplicationStatus

class SparkAppStatusIconRenderer : AbstractIconRenderer(defaultIcons, AllIcons.RunConfigurations.TestUnknown) {
  companion object {
    val defaultIcons = ApplicationStatus.entries.associateWith {
      when (it) {
        ApplicationStatus.COMPLETE -> AllIcons.RunConfigurations.TestPassed
        ApplicationStatus.RUNNING -> AllIcons.RunConfigurations.TestState.Run_run
        ApplicationStatus.STARTING -> AnimatedIcon.Default()
        ApplicationStatus.ERROR -> AllIcons.RunConfigurations.TestFailed
        ApplicationStatus.UNKNOWN -> null
      }
    }.map { it.key.name to it.value }.toMap()
  }
}