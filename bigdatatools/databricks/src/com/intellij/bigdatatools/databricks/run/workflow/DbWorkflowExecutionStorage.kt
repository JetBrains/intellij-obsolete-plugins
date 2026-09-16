package com.intellij.bigdatatools.databricks.run.workflow

import com.databricks.sdk.service.jobs.ExportRunOutput
import com.databricks.sdk.service.jobs.Run
import com.databricks.sdk.service.jobs.RunOutput
import com.databricks.sdk.service.jobs.SubmitRun
import com.databricks.sdk.service.jobs.SubmitRunResponse
import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.WorkflowRunInfo
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver
import com.intellij.bigdatatools.databricks.toolwindow.DatabricksMonitoringToolWindowController
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.util.concurrent.ConcurrentSkipListMap

internal class DbWorkflowExecutionStorage(val dataManager: DatabricksDataManager) {
  private val tasks = ConcurrentSkipListMap<Long, WorkflowRunInfo>()

  fun getInfos() = tasks.values.sortedByDescending { it.startTime }.toList()

  fun getInfo(runId: Long): WorkflowRunInfo? {
    return tasks[runId]
  }

  fun init(fileName: String, project: Project, request: SubmitRun, response: SubmitRunResponse, initInfo: Run) {
    val runId = response.runId
    tasks[runId] = WorkflowRunInfo(fileName = fileName, runId = runId, request = request, info = initInfo)
    dataManager.refreshWorkspaceExecutions(runId)
    invokeLater {
      DatabricksMonitoringToolWindowController.getInstance(project)
        ?.focusOn(dataManager.connectionId, DatabricksDriver.workflowRunPath.addRelative(runId.toString(), isDirectory = false))

    }
  }

  fun updateInfo(runId: Long, run: Run) {
    val current = tasks[runId] ?: error("Context is not inited")
    val runInfo = current.copy(info = run)
    tasks[runId] = runInfo
    dataManager.refreshWorkspaceExecutions(runId)
  }


  fun updateResult(runId: Long, jobRunOutput: RunOutput?, notebookResult: ExportRunOutput?) {
    val current = tasks[runId] ?: error("Context is not inited")
    val runInfo = current.copy(runOutput = jobRunOutput, exportResult = notebookResult)
    tasks[runId] = runInfo
    dataManager.refreshWorkspaceExecutions(runId)
  }
}