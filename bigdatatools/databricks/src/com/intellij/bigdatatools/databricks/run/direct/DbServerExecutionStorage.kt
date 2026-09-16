package com.intellij.bigdatatools.databricks.run.direct

import com.databricks.sdk.service.compute.CommandStatusResponse
import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.ServerRunInfo
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver
import com.intellij.bigdatatools.databricks.toolwindow.DatabricksMonitoringToolWindowController
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.util.concurrent.ConcurrentSkipListMap

internal class DbServerExecutionStorage(val dataManager: DatabricksDataManager) {
  private val tasks = ConcurrentSkipListMap<String, ServerRunInfo>()

  fun getInfos() = tasks.values.sortedByDescending { it.startTime }.toList()

  fun getInfo(contextId: String): ServerRunInfo? {
    return tasks[contextId]
  }

  fun init(fileName: String, project: Project, contextId: String, command: String) {
    tasks[contextId] = ServerRunInfo(fileName = fileName, startTime = System.currentTimeMillis(), contextId = contextId, command = command)
    dataManager.refreshServerExecutions(contextId)

    invokeLater {
      DatabricksMonitoringToolWindowController.getInstance(project)
        ?.focusOn(dataManager.connectionId, DatabricksDriver.serverRunPath.addRelative(contextId, isDirectory = false))
    }
  }

  fun setError(contextId: String, throwable: Throwable) {
    val current = tasks[contextId] ?: error("Context is not inited")

    val runInfo = current.copy(error = throwable, response = null)
    tasks[contextId] = runInfo
    if (current != runInfo) {
      dataManager.refreshServerExecutions(contextId)
    }
  }

  fun updateResult(contextId: String, commandStatusResponse: CommandStatusResponse) {
    val current = tasks[contextId] ?: error("Context is not inited")
    val runInfo = current.copy(response = commandStatusResponse, error = null)
    tasks[contextId] = runInfo
    dataManager.refreshServerExecutions(contextId)
  }
}