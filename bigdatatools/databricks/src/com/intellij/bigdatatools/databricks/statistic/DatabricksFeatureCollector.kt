package com.intellij.bigdatatools.databricks.statistic

import com.databricks.sdk.service.compute.CommandStatus
import com.databricks.sdk.service.compute.CommandStatusResponse
import com.databricks.sdk.service.compute.ResultType
import com.databricks.sdk.service.jobs.Run
import com.databricks.sdk.service.jobs.RunResultState
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.bigdatatools.databricks.sync.SyncStatus
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector

internal object DatabricksFeatureCollector : CounterUsagesCollector() {
  override fun getGroup() = GROUP

  private val GROUP = EventLogGroup("databricks.features", 2)

  private val duration = EventFields.RoundedInt("duration_sec")
  private val serverCommandExecStatus = EventFields.Enum<CommandStatus>("status")
  private val serverCommandResultType = EventFields.Enum<ResultType>("result_type")
  private val workflowResultState = EventFields.Enum<RunResultState>("result_state")
  private val workflowIsNotebook = EventFields.Boolean("is_notebook")
  private val syncStatus = EventFields.Enum<SyncStatus>("status")

  private val serverExecResultEvent = GROUP.registerEvent("server.job.executed", duration, serverCommandExecStatus, serverCommandResultType)
  private val serverExecExceptionEvent = GROUP.registerEvent("server.job.exec.exception")
  private val workflowResultEvent = GROUP.registerEvent("server.job.executed", duration, workflowResultState, workflowIsNotebook)
  private val syncStatusEvent = GROUP.registerEvent("workspace.sync.status.updated", syncStatus)

  fun registerWorkflowFinished(commandStatusResponse: Run) = executeOnPooledThread {
    val task = commandStatusResponse.tasks.firstOrNull() ?: return@executeOnPooledThread
    val resultState = task.state?.resultState ?: return@executeOnPooledThread
    val duration = ((commandStatusResponse.endTime - commandStatusResponse.startTime) / 1000).toInt()
    val isNotebook = task.notebookTask != null
    workflowResultEvent.log(duration, resultState, isNotebook)
  }

  fun serverCommandThrowException() = executeOnPooledThread {
    serverExecExceptionEvent.log()
  }

  fun serverCommandExecuted(commandStatusResponse: CommandStatusResponse, executionTime: Long) = executeOnPooledThread {
    serverExecResultEvent.log((executionTime / 1000).toInt(),
                              commandStatusResponse.status,
                              commandStatusResponse.results?.resultType ?: ResultType.TEXT)
  }

  fun syncDirectoryUpdated(status: SyncStatus) = executeOnPooledThread {
    syncStatusEvent.log(status)
  }
}