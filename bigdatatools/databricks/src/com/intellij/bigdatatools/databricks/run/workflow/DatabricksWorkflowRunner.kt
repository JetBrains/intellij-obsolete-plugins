package com.intellij.bigdatatools.databricks.run.workflow

import com.databricks.sdk.service.jobs.NotebookTask
import com.databricks.sdk.service.jobs.SparkPythonTask
import com.databricks.sdk.service.jobs.SubmitRun
import com.databricks.sdk.service.jobs.SubmitTask
import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.ClusterInfoPresentable
import com.intellij.bigdatatools.databricks.model.isTerminated
import com.intellij.bigdatatools.databricks.run.common.DatabricksAbstractRunner
import com.intellij.bigdatatools.databricks.run.common.DatabricksRunType
import com.intellij.bigdatatools.databricks.statistic.DatabricksFeatureCollector
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import kotlinx.coroutines.delay

internal class DatabricksWorkflowRunner(project: Project, dataManager: DatabricksDataManager) : DatabricksAbstractRunner(project, dataManager) {
  private val wrapper = WorkspaceWorkflowWrapper(project, dataManager)

  val storage = dataManager.workflowExecutionStorage

  override suspend fun runPyFile(file: VirtualFile,
                                 rfsPath: RfsPath,
                                 cluster: ClusterInfoPresentable,
                                 args: List<String>,
                                 env: Map<String, Any>) {
    val request = prepareRunPyRequest(file, rfsPath, cluster)
    runWorkflow(file.name, request, isNotebook = false)
  }

  override suspend fun runNotebook(file: VirtualFile,
                                   rfsPath: RfsPath,
                                   clusterInfoPresentable: ClusterInfoPresentable) {
    val request = prepareNotebookRequest(file, rfsPath, clusterInfoPresentable)
    runWorkflow(file.name, request, isNotebook = true)
  }

  override fun detectFileType(file: VirtualFile): DatabricksRunType? {
    if (file.extension == "ipynb") {
      return DatabricksRunType.NOTEBOOK
    }
    if (file.extension != "py") {
      return null
    }

    val firstLine = file.inputStream.bufferedReader().readLine()

    return if (firstLine.contains("# Databricks notebook source")) {
      DatabricksRunType.NOTEBOOK
    }
    else {
      DatabricksRunType.PY_FILE
    }
  }


  private suspend fun runWorkflow(fileName: String, request: SubmitRun, isNotebook: Boolean) {
    val runResponse = dataManager.runJob(project, request)
    val runId = runResponse.runId
    val initInfo = dataManager.client.getJobRun(runId)
    storage.init(fileName, project, request, runResponse, initInfo)
    while (true) {
      val commandStatusResponse = dataManager.client.getJobRun(runId)
      val isFinished = commandStatusResponse.state.lifeCycleState.isTerminated
      storage.updateInfo(runId, commandStatusResponse)

      if (isFinished) {
        DatabricksFeatureCollector.registerWorkflowFinished(commandStatusResponse)
        if (isNotebook) {
          val exportRunOutput = dataManager.client.exportJobRunOutput(runId)
          storage.updateResult(runId, null, exportRunOutput)
        }
        else {
          val taskId = commandStatusResponse.tasks.firstOrNull()?.runId
          val jobRunOutput = taskId?.let { dataManager.client.getJobRunOutput(it) }
          storage.updateResult(runId, jobRunOutput, null)
        }
        return
      }
      else {
        delay(1000)
      }
    }
  }

  private fun prepareRunPyRequest(file: VirtualFile,
                                  rfsPath: RfsPath,
                                  cluster: ClusterInfoPresentable): SubmitRun {
    val wrapperPath = wrapper.createPyFileWrapper(rfsPath)

    val task = SparkPythonTask().apply {
      pythonFile = wrapperPath.stringRepresentation()
    }

    val submitTask = SubmitTask().apply {
      taskKey = "databricks_intellij_job_run"
      existingClusterId = cluster.id
      sparkPythonTask = task
    }

    return SubmitRun().apply {
      runName = "${file.name} IJ Job Run"
      setTasks(listOf(submitTask))
    }
  }

  private fun prepareNotebookRequest(file: VirtualFile,
                                     rfsPath: RfsPath,
                                     clusterInfoPresentable: ClusterInfoPresentable): SubmitRun {
    val wrapperPath = wrapper.createIpynbWrapper(file, rfsPath)

    val notebookTask = NotebookTask().apply {
      notebookPath = wrapperPath.stringRepresentation()
    }

    val submitTask = SubmitTask().apply {
      this.notebookTask = notebookTask
      taskKey = "databricks_intellij_job_run"
      existingClusterId = clusterInfoPresentable.id
    }

    return SubmitRun().apply {
      runName = "${file.name} IJ Job Run"
      setTasks(listOf(submitTask))
    }
  }
}