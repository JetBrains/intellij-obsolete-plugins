package com.jetbrains.bigdatatools.flink.model

import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.flink.util.FlinkLocalizedColumn

data class JobExceptionType(
  val exception: String?,
  val task: String?,
  val location: String?,
  @field:DateRendering(neededAddChecking = true)
  val timestamp: Long?
) : RemoteInfo {
  companion object {
    val renderableColumns: List<FlinkLocalizedColumn<JobExceptionType>> by lazy {
      listOf(
        FlinkLocalizedColumn(JobExceptionType::exception, "data.JobExceptionType.exception"),
        FlinkLocalizedColumn(JobExceptionType::task, "data.JobExceptionType.task"),
        FlinkLocalizedColumn(JobExceptionType::location, "data.JobExceptionType.location"),
        FlinkLocalizedColumn(JobExceptionType::timestamp, "data.JobExceptionType.timestamp")
      )
    }
  }
}