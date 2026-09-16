package com.intellij.bigdatatools.databricks.model

import com.databricks.sdk.service.compute.CommandStatus
import com.databricks.sdk.service.compute.CommandStatusResponse
import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.intellij.bigdatatools.coreUi.table.renderers.UnixtimeRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.common.util.TimeUtils

data class ServerRunInfo(
  val fileName: String,
  @UnixtimeRendering
  val startTime: Long,
  val contextId: String,
  @NoRendering val response: CommandStatusResponse? = null,
  val error: Throwable? = null,
  val command: String) : RemoteInfo {
  val status
    get() = response?.status ?: CommandStatus.QUEUED

  val formatedStartTime: String = TimeUtils.unixTimeToString(startTime)

  /** We will save end time on this data class creation. This is not super precise, but this is a fastest way. */
  val endTime = System.currentTimeMillis()
}