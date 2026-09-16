package com.jetbrains.spark.monitoring.data

import com.intellij.bigdatatools.coreUi.table.renderers.CustomRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.intellij.bigdatatools.coreUi.table.renderers.LinkRendering
import com.intellij.bigdatatools.coreUi.table.renderers.ProgressRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.spark.monitoring.ui.table.renderers.SparkJobStatusIconRenderer
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import com.jetbrains.spark.monitoring.util.SparkLocalizedField
import com.squareup.moshi.Json
import java.util.Date


/*
http://localhost:4040/api/v1/applications/local-1558339557699/jobs

[ {
  "jobId" : 1,
  "name" : "count at <console>:36",
  "submissionTime" : "2019-05-20T08:06:07.200GMT",
  "completionTime" : "2019-05-20T08:06:08.912GMT",
  "stageIds" : [ 1, 2, 3 ],
  "jobGroup" : "zeppelin-2A94M5J1Z-paragraph_1557846455650_-1945831503",
  "status" : "SUCCEEDED",
  "numTasks" : 202,
  "numActiveTasks" : 0,
  "numCompletedTasks" : 202,
  "numSkippedTasks" : 0,
  "numFailedTasks" : 0,
  "numKilledTasks" : 0,
  "numCompletedIndices" : 202,
  "numActiveStages" : 0,
  "numCompletedStages" : 3,
  "numSkippedStages" : 0,
  "numFailedStages" : 0,
  "killedTasksSummary" : { }
} ]
 */
// @JsonClass(generateAdapter = true)
@Suppress("unused")
data class JobData(
  @field:Json(name = "jobId") @Json(name = "jobId") val id: Int,
  val name: String,
  @field:DateRendering val submissionTime: Date? = null,
  @field:DateRendering var completionTime: Date? = null,
  val stageIds: List<Int> = emptyList(),
  val jobGroup: String = "",
  val numTasks: Int = 0,
  val numActiveTasks: Int = 0,
  @field:ProgressRendering("totalTasks") val numCompletedTasks: Int = 0,
  val numSkippedTasks: Int = 0,
  val numFailedTasks: Int = 0,
  val numKilledTasks: Int = 0,
  val numCompletedIndices: Int = 0,
  val numActiveStages: Int = 0,
  val numCompletedStages: Int = 0,
  val numSkippedStages: Int = 0,
  val numFailedStages: Int = 0,
  val killedTasksSummary: Map<String,Int> = mapOf(),
  @field:CustomRendering(SparkJobStatusIconRenderer::class) var status: JobExecutionStatus = JobExecutionStatus.UNKNOWN
) : RemoteInfo {


  @LinkRendering
  val visualization: String = SMMessagesBundle.message("action.show.execution.graph")
  val totalStages: Int get() = numCompletedStages + numActiveStages + numSkippedStages + numFailedStages
  val totalTasks: Int get() = numCompletedTasks + numActiveTasks + numFailedTasks + numKilledTasks

  companion object {
    val renderableColumns: List<SparkLocalizedField<JobData>> by lazy {
      listOf(
        SparkLocalizedField(JobData::id, "data.job.id"),
        SparkLocalizedField(JobData::status, "data.job.status"),
        SparkLocalizedField(JobData::name, "data.job.name"),
        SparkLocalizedField(JobData::submissionTime, "data.job.submissionTime"),
        SparkLocalizedField(JobData::completionTime, "data.job.completionTime"),
        SparkLocalizedField(JobData::stageIds, "data.job.stageIds"),
        SparkLocalizedField(JobData::jobGroup, "data.job.jobGroup"),
        SparkLocalizedField(JobData::numTasks, "data.job.numTasks"),
        SparkLocalizedField(JobData::numActiveTasks, "data.job.numActiveTasks"),
        SparkLocalizedField(JobData::numCompletedTasks, "data.job.numCompletedTasks"),
        SparkLocalizedField(JobData::numSkippedTasks, "data.job.numSkippedTasks"),
        SparkLocalizedField(JobData::numFailedTasks, "data.job.numFailedTasks"),
        SparkLocalizedField(JobData::numKilledTasks, "data.job.numKilledTasks"),
        SparkLocalizedField(JobData::numCompletedIndices, "data.job.numCompletedIndices"),
        SparkLocalizedField(JobData::numActiveStages, "data.job.numActiveStages"),
        SparkLocalizedField(JobData::numCompletedStages, "data.job.numCompletedStages"),
        SparkLocalizedField(JobData::numSkippedStages, "data.job.numSkippedStages"),
        SparkLocalizedField(JobData::numFailedStages, "data.job.numFailedStages"),
        SparkLocalizedField(JobData::killedTasksSummary, "data.job.killedTasksSummary"),
        SparkLocalizedField(JobData::visualization, "data.job.visualization"),
        SparkLocalizedField(JobData::totalStages, "data.job.totalStages"),
        SparkLocalizedField(JobData::totalTasks, "data.job.totalTasks")
      )
    }
  }
}