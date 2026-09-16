package com.intellij.bigdatatools.databricks.model

import com.databricks.sdk.service.jobs.ExportRunOutput
import com.databricks.sdk.service.jobs.Run
import com.databricks.sdk.service.jobs.RunLifeCycleState
import com.databricks.sdk.service.jobs.RunOutput
import com.databricks.sdk.service.jobs.SubmitRun
import com.intellij.bigdatatools.coreUi.table.renderers.DurationRendering
import com.intellij.bigdatatools.coreUi.table.renderers.UnixtimeRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.common.util.TimeUtils

data class WorkflowRunInfo(
  val fileName: String,
  val runId: Long,
  val request: SubmitRun,
  val info: Run,
  val runOutput: RunOutput? = null,
  val exportResult: ExportRunOutput? = null,
) : RemoteInfo {
  val status: RunLifeCycleState = info.state.lifeCycleState
  val isRunning: Boolean = !status.isTerminated

  @UnixtimeRendering
  val startTime: Long = info.startTime

  val formatedStartTime: String = TimeUtils.unixTimeToString(startTime)

  @DurationRendering
  val duration: Long? = info.runDuration

  /** We are storing object creation time, because the duration from server can be missing. */
  val endTime = System.currentTimeMillis()
}