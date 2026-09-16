package com.jetbrains.bigdatatools.flink.model

import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.flink.util.FlinkLocalizedColumn

data class JobExceptionEntry(
  val concurrentExceptions: List<JobExceptionDataWithStackTrace> = emptyList(),
  val exceptionName: String?,
  val location: String?,
  val stacktrace: String?,
  val taskName: String?,
  @field:DateRendering(neededAddChecking = true)
  val timestamp: Long?
): RemoteInfo {
  companion object {
    val renderableColumns: List<FlinkLocalizedColumn<JobExceptionEntry>> by lazy {
      listOf(
        FlinkLocalizedColumn(JobExceptionEntry::concurrentExceptions, "data.JobExceptionEntry.concurrentExceptions"),
        FlinkLocalizedColumn(JobExceptionEntry::exceptionName, "data.JobExceptionEntry.exceptionName"),
        FlinkLocalizedColumn(JobExceptionEntry::location, "data.JobExceptionEntry.location"),
        FlinkLocalizedColumn(JobExceptionEntry::stacktrace, "data.JobExceptionEntry.stacktrace"),
        FlinkLocalizedColumn(JobExceptionEntry::taskName, "data.JobExceptionEntry.taskName"),
        FlinkLocalizedColumn(JobExceptionEntry::timestamp, "data.JobExceptionEntry.timestamp")
      )
    }
  }
}