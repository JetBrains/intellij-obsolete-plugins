package com.intellij.bigdatatools.databricks.model

import com.databricks.sdk.service.jobs.JobSettings
import com.intellij.bigdatatools.databricks.util.DatabricksLocalizedField
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.squareup.moshi.Json

data class JobInfo(
  @Json(name = "job_id")
  val jobId: String = "",
  @Json(name = "creator_user_name")
  val creatorUserName: String = "",
  @Json(name = "settings")
  val settings: JobSettings = JobSettings(),
  @Json(name = "created_time")
  val createdTime: Long = 0
) : RemoteInfo {
  companion object {
    val renderableColumns: List<DatabricksLocalizedField<JobInfo>> by lazy {
      listOf(
        DatabricksLocalizedField(JobInfo::jobId, "data.JobInfo.jobId"),
        DatabricksLocalizedField(JobInfo::creatorUserName, "data.JobInfo.creatorUserName"),
        DatabricksLocalizedField(JobInfo::settings, "data.JobInfo.settings"),
        DatabricksLocalizedField(JobInfo::createdTime, "data.JobInfo.createdTime"),
      )
    }
  }
}
