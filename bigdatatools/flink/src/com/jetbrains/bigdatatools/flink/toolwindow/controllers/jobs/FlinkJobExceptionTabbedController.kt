package com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobs

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TabbedDetailsMonitoringController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle

class FlinkJobExceptionTabbedController(project: Project, dataManager: FlinkDataManager) : TabbedDetailsMonitoringController<String>(
  project) {
  override val tabsControllers: List<Pair<String, DetailsMonitoringController<String>>> = listOf(
    FlinkMessagesBundle.message("job.exception.all.exceptions.text") to FlinkJobAllExceptionsController(dataManager),
    FlinkMessagesBundle.message("job.exception.history.text") to FlinkJobExceptionHistoryController(dataManager)
  )

  init {
    init()
  }
}

