package com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobmanager

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.toolwindow.controllers.ConsoleLogsMonitoringController

class FlinkJobMangerStdOutController(project: Project,
                                     dataManager: FlinkDataManager) : ConsoleLogsMonitoringController(project, dataManager) {
  override val downloadFileName = { "jobManager_stdout.txt" }

  init {
    setupBaseModel()
  }

  override fun getAdditionalActions() = listOf(OpenUrlAction(dataManager) { "/#/job-manager/stdout" })

  override fun getDataModel() = dataManager.getJobMangerStdOutModel()
}