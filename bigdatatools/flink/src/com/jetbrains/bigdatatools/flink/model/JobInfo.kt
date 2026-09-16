package com.jetbrains.bigdatatools.flink.model

import com.intellij.bigdatatools.coreUi.table.renderers.CustomRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DurationRendering
import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.flink.util.FlinkLocalizedColumn
import com.jetbrains.bigdatatools.flink.util.FlinkStatusIconRenderer
import com.squareup.moshi.Json

data class JobInfo(
  @field:NoRendering
  val jid: String,
  @Json(name = "name")
  val jobName: String,
  @field:CustomRendering(FlinkStatusIconRenderer::class)
  @Json(name = "state")
  val status: JobExecutionStatus,
  @field:DateRendering(neededAddChecking = true)
  @Json(name = "start-time")
  val startTime: Long = 0,
  @field:DateRendering(neededAddChecking = true)
  @Json(name = "end-time")
  val endTime: Long = 0,
  @field:DurationRendering(neededAddChecking = true)
  val duration: Long = 0,
  @Json(name = "last-modification")
  var lastModification: Long = 0,
  val tasks: TaskInfo
): RemoteInfo {
  val visualization: String = "Show"

  companion object {
    val STATUS_FILTER = FilterKey("status")
    val LIMIT_FILTER = FilterKey("limit")
    val TEXT_FILTER = FilterKey("filterText")

    val renderableColumns: List<FlinkLocalizedColumn<JobInfo>> by lazy {
      listOf(
        FlinkLocalizedColumn(JobInfo::jobName, "data.JobInfo.jobName"),
        FlinkLocalizedColumn(JobInfo::status, "data.JobInfo.status"),
        FlinkLocalizedColumn(JobInfo::startTime, "data.JobInfo.startTime"),
        FlinkLocalizedColumn(JobInfo::endTime, "data.JobInfo.endTime"),
        FlinkLocalizedColumn(JobInfo::duration, "data.JobInfo.duration"),
        FlinkLocalizedColumn(JobInfo::lastModification, "data.JobInfo.lastModification"),
        FlinkLocalizedColumn(JobInfo::tasks, "data.JobInfo.tasks"),
        FlinkLocalizedColumn(JobInfo::visualization, "data.JobInfo.visualization")
      )
    }
  }
}