package com.jetbrains.bigdatatools.flink.toolwindow.controllers.taskmanager

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.toolwindow.controllers.ConsoleLogsDetailsMonitoringController

class FlinkTaskMangerStdOutController(project: Project,
                                      dataManager: FlinkDataManager) : ConsoleLogsDetailsMonitoringController(project, dataManager) {
  override val downloadFileName = { "taskManager_stdout" }

  override fun getAdditionalActions() = listOf(
    OpenUrlAction(dataManager) {
      val taskManagersId = selectedId ?: return@OpenUrlAction null
      "/#/task-manager/$taskManagersId/stdout"
    }
  )

  override fun getDataModel() = selectedId?.let { dataManager.getTaskManagerStdOutModel(it) }
}