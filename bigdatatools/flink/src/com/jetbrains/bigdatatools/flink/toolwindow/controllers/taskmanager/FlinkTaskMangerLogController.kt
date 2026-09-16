package com.jetbrains.bigdatatools.flink.toolwindow.controllers.taskmanager

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.toolwindow.controllers.ConsoleLogsDetailsMonitoringController

class FlinkTaskMangerLogController(project: Project,
                                   dataManager: FlinkDataManager) : ConsoleLogsDetailsMonitoringController(project, dataManager) {
  override val downloadFileName = { "taskManager_log" }

  override fun getAdditionalActions() = listOf(
    OpenUrlAction(dataManager) {
      val taskManagersId = selectedId ?: return@OpenUrlAction null
      "/#/task-manager/$taskManagersId/logs"
    }
  )

  override fun getDataModel() = selectedId?.let { dataManager.getTaskManagerLogModel(it) }
}