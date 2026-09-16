package com.intellij.bigdatatools.plugin.spark.submit.gutter

import com.intellij.execution.Location
import com.intellij.execution.RunManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.spark.submit.run.cluster.ClusterSparkJobRunConfiguration
import com.jetbrains.spark.submit.util.SparkMessagesBundle

internal class SubmitRunActionGroup : ActionGroup() {
  var location: Location<*>? = null

  override fun update(e: AnActionEvent) {
    val location = e.getData(Location.DATA_KEY)
    if (location != null) {
      this.location = location
    }
    if (getChildren(e).isEmpty()) {
      e.presentation.text = SparkMessagesBundle.message("spark.submit.gutter.icon.tooltip")
      e.presentation.isPopupGroup = true
      e.presentation.isPerformGroup = true
      e.presentation.isEnabled = false
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun getChildren(e: AnActionEvent?): Array<AnAction> {
    val project = e?.project ?: return emptyArray()
    val location = e.getData(Location.DATA_KEY) ?: this.location ?: return emptyArray()
    val sparkConfigurationProducer = SparkConfigurationProducerProvider.findSparkConfigurationProducer(location) ?: return emptyArray()
    return RunManager.getInstance(project).allSettings.mapNotNull { configurationSettings ->
      val configuration = configurationSettings.configuration
      if (configuration !is ClusterSparkJobRunConfiguration) return@mapNotNull null
      if (!sparkConfigurationProducer.isFromContext(configuration)) return@mapNotNull null
      SubmitConfigurationAction(project, configurationSettings)
    }.toTypedArray()
  }
}