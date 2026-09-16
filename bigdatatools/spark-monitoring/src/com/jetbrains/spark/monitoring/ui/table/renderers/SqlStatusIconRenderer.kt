package com.jetbrains.spark.monitoring.ui.table.renderers

import com.intellij.icons.AllIcons
import com.intellij.ui.AnimatedIcon
import com.jetbrains.bigdatatools.common.table.renderers.AbstractIconRenderer

class SqlStatusIconRenderer : AbstractIconRenderer(
  defaultIcons, AllIcons.RunConfigurations.TestUnknown) {
  companion object {
    val defaultIcons = mapOf(
      "COMPLETED" to AllIcons.RunConfigurations.TestPassed,
      "RUNNING" to AnimatedIcon.Default(),
      "FAILED" to AllIcons.RunConfigurations.TestFailed
    )
  }
}