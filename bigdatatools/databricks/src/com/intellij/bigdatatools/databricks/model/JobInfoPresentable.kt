package com.intellij.bigdatatools.databricks.model

import com.databricks.sdk.service.jobs.BaseJob
import com.intellij.bigdatatools.databricks.util.DatabricksLocalizedField
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo

data class JobInfoPresentable(
  val name: String,
  val jobId: Long,
  val createdBy: String,
  val schedule: String,
  val lastRun: String
) : RemoteInfo {
  companion object {
    fun createFrom(jobInfo: BaseJob): JobInfoPresentable = JobInfoPresentable(
      name = jobInfo.settings.name,
      jobId = jobInfo.jobId,
      createdBy = jobInfo.creatorUserName,
      schedule = jobInfo.settings.schedule?.toString() ?: "None",
      lastRun = "<NOT_DEFINED>"
    )


    val renderableColumns: List<DatabricksLocalizedField<JobInfoPresentable>> by lazy {
      listOf(
        DatabricksLocalizedField(JobInfoPresentable::name, "data.JobInfoPresentable.name"),
        DatabricksLocalizedField(JobInfoPresentable::jobId, "data.JobInfoPresentable.jobId"),
        DatabricksLocalizedField(JobInfoPresentable::createdBy, "data.JobInfoPresentable.createdBy"),
        DatabricksLocalizedField(JobInfoPresentable::schedule, "data.JobInfoPresentable.schedule"),
        DatabricksLocalizedField(JobInfoPresentable::lastRun, "data.JobInfoPresentable.lastRun")
      )
    }
  }
}