package com.jetbrains.bigdatatools.flink.toolwindow.controllers.taskmanager

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.toolwindow.controllers.ConsoleLogsDetailsMonitoringController

class FlinkTaskMangerThreadDumpController(project: Project,
                                          dataManager: FlinkDataManager) :
  ConsoleLogsDetailsMonitoringController(project, dataManager, isFormatted = true) {
  override val downloadFileName = { "taskManager_thread_dump" }

  override fun getAdditionalActions() = listOf(
    OpenUrlAction(dataManager) {
      val taskManagersId = selectedId ?: return@OpenUrlAction null
      "/#/task-manager/$taskManagersId/thread-dump"
    }
  )

  override fun getDataModel() = selectedId?.let { dataManager.getTaskManagerThreadDumpModel(it) }
}