package com.jetbrains.bigdatatools.flink.model

import com.intellij.bigdatatools.coreUi.table.renderers.CustomRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DataSizeRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DurationRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.flink.util.FlinkLocalizedColumn
import com.jetbrains.bigdatatools.flink.util.FlinkStatusIconRenderer
import com.squareup.moshi.Json

data class JobVertex(
  @field:DurationRendering(neededAddChecking = true)
  val duration: Long,
  @field:DateRendering(neededAddChecking = true)
  @Json(name = "end-time")
  val endTime: Long,
  val id: String,
  val maxParallelism: Long? = null,
  val metrics: JobVertexMetrics,
  val name: String,
  val parallelism: Long? = null,
  @field:DateRendering(neededAddChecking = true)
  @Json(name = "start-time")
  val startTime: Long,
  @field:CustomRendering(FlinkStatusIconRenderer::class)
  val status: JobExecutionStatus,
  val tasks: Any?
) : RemoteInfo {
  @field:DataSizeRendering
  val bytesReceived: Long = metrics.bytesReceived
  val recordsReceived = metrics.recordsReceived
  @field:DataSizeRendering
  val bytesSent: Long = metrics.bytesSent
  val recordsSent = metrics.recordsSent
  companion object {
    val renderableColumns: List<FlinkLocalizedColumn<JobVertex>> by lazy {
      listOf(
        FlinkLocalizedColumn(JobVertex::duration, "data.JobVertex.duration"),
        FlinkLocalizedColumn(JobVertex::id, "data.JobVertex.id"),
        FlinkLocalizedColumn(JobVertex::maxParallelism, "data.JobVertex.maxParallelism"),
        FlinkLocalizedColumn(JobVertex::metrics, "data.JobVertex.metrics"),
        FlinkLocalizedColumn(JobVertex::name, "data.JobVertex.name"),
        FlinkLocalizedColumn(JobVertex::parallelism, "data.JobVertex.parallelism"),
        FlinkLocalizedColumn(JobVertex::startTime, "data.JobVertex.startTime"),
        FlinkLocalizedColumn(JobVertex::status, "data.JobVertex.status"),
        FlinkLocalizedColumn(JobVertex::tasks, "data.JobVertex.tasks"),
        FlinkLocalizedColumn(JobVertex::bytesReceived, "data.JobVertex.bytesReceived"),
        FlinkLocalizedColumn(JobVertex::recordsReceived, "data.JobVertex.recordsReceived"),
        FlinkLocalizedColumn(JobVertex::bytesSent, "data.JobVertex.bytesSent"),
        FlinkLocalizedColumn(JobVertex::recordsSent, "data.JobVertex.recordsSent")
      )
    }
  }
}