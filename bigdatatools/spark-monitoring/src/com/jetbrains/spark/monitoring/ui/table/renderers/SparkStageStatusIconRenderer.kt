package com.jetbrains.spark.monitoring.ui.table.renderers

import com.intellij.icons.AllIcons
import com.intellij.ui.AnimatedIcon
import com.jetbrains.bigdatatools.common.table.renderers.AbstractIconRenderer
import com.jetbrains.spark.monitoring.data.StageStatus

class SparkStageStatusIconRenderer : AbstractIconRenderer(defaultIcons, AllIcons.RunConfigurations.TestUnknown) {
  companion object {
    val defaultIcons = StageStatus.entries.associateWith {
      when (it) {
        StageStatus.ACTIVE -> AllIcons.RunConfigurations.TestState.Run_run
        StageStatus.COMPLETE -> AllIcons.RunConfigurations.TestPassed
        StageStatus.PENDING -> AnimatedIcon.Default()
        StageStatus.FAILED -> AllIcons.RunConfigurations.TestFailed
        StageStatus.SKIPPED -> AllIcons.RunConfigurations.TestSkipped
      }
    }.map { it.key.name to it.value }.toMap()
  }
}