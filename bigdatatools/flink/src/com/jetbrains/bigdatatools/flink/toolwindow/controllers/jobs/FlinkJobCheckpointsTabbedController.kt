package com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobs

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TabbedDetailsMonitoringController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle

class FlinkJobCheckpointsTabbedController(project: Project, dataManager: FlinkDataManager) : TabbedDetailsMonitoringController<String>(
  project) {
  override val tabsControllers: List<Pair<String, DetailsMonitoringController<String>>> = listOf(
    FlinkMessagesBundle.message("job.checkpoints.tabbed.overview") to FlinkJobCheckpointsOverviewController(project, dataManager),
    FlinkMessagesBundle.message("job.checkpoints.tabbed.history") to FlinkJobCheckpointsHistoryController(dataManager),
    FlinkMessagesBundle.message("job.checkpoints.tabbed.summary") to FlinkJobCheckpointsSummaryController(dataManager),
    FlinkMessagesBundle.message("job.checkpoints.tabbed.configuration") to FlinkJobCheckpointsConfigController(project, dataManager)
  )

  init {
    init()
  }
}

