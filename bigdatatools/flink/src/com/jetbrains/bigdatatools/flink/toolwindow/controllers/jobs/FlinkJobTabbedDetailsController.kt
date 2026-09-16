package com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobs

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TabbedDetailsMonitoringController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle

class FlinkJobTabbedDetailsController(project: Project, dataManager: FlinkDataManager) : TabbedDetailsMonitoringController<String>(
  project) {
  private val flinkTabs = listOf(
    FlinkMessagesBundle.message("job.details.tabbed.overview") to FlinkJobOverviewController(dataManager),
    FlinkMessagesBundle.message("job.details.tabbed.exception") to FlinkJobExceptionTabbedController(project, dataManager),
    FlinkMessagesBundle.message("job.details.tabbed.configuration") to FlinkJobConfigController(project, dataManager),
    FlinkMessagesBundle.message("job.details.tabbed.checkpoints") to FlinkJobCheckpointsTabbedController(project, dataManager)
  )

  override val tabsControllers =
    if (dataManager.connectionData.isFlinkHistory) flinkTabs.dropLast(1) else flinkTabs

  init {
    init()
  }
}
