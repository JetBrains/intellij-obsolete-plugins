package com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobmanager

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TabbedMonitoringController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle

class FlinkJobMangerController(project: Project,
                               dataManager: FlinkDataManager) : TabbedMonitoringController(project) {
  override val tabsControllers = listOf(
    FlinkMessagesBundle.message("jobManager.configuration.text") to FlinkJobManagerConfigController(project, dataManager),
    FlinkMessagesBundle.message("jobManager.logs.text") to FlinkJobMangerLogController(project, dataManager),
    FlinkMessagesBundle.message("jobManager.stdout.text") to FlinkJobMangerStdOutController(project, dataManager),
    FlinkMessagesBundle.message("jobManager.logList.text") to FlinkJobManagerLogListController(project, dataManager)
  )

  init {
    tabsControllers.forEach {
      Disposer.register(this, it.second)
    }
    init()
  }
}