package com.jetbrains.bigdatatools.flink.toolwindow.controllers.taskmanager

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TabbedDetailsMonitoringController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle

class FlinkTaskManagerTabbedDetailsController(project: Project,
                                              dataManager: FlinkDataManager) : TabbedDetailsMonitoringController<String>(project) {
  override val tabsControllers: List<Pair<String, DetailsMonitoringController<String>>> = listOf(
    FlinkMessagesBundle.message("taskManager.logs.text") to FlinkTaskMangerLogController(project, dataManager),
    FlinkMessagesBundle.message("taskManager.stdout.text") to FlinkTaskMangerStdOutController(project, dataManager),
    FlinkMessagesBundle.message("taskManager.logList.text") to FlinkTaskManagerLogListController(project, dataManager),
    FlinkMessagesBundle.message("taskManager.threadDump.text") to FlinkTaskMangerThreadDumpController(project, dataManager),
  )

  init {
    tabsControllers.forEach {
      Disposer.register(this, it.second)
    }
    init()
  }
}


