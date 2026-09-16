package com.jetbrains.bigdatatools.flink.model

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.flink.util.FlinkLocalizedColumn
import com.squareup.moshi.Json

data class JobExecutionConfig(
  @Json(name = "execution-mode")
  val executionMode: String,
  @Json(name = "restart-strategy")
  val restartStrategy: String,
  @Json(name = "job-parallelism")
  val jobParallelism: Long? = null,
  @Json(name = "object-reuse-mode")
  val objectReuseMode: Boolean,
  @Json(name = "user-config")
  val userConfig: Any?
) : RemoteInfo {
  companion object {
    val renderableColumns: List<FlinkLocalizedColumn<JobExecutionConfig>> by lazy {
      listOf(
        FlinkLocalizedColumn(JobExecutionConfig::executionMode, "data.JobExecutionConfig.executionMode"),
        FlinkLocalizedColumn(JobExecutionConfig::restartStrategy, "data.JobExecutionConfig.restartStrategy"),
        FlinkLocalizedColumn(JobExecutionConfig::jobParallelism, "data.JobExecutionConfig.jobParallelism"),
        FlinkLocalizedColumn(JobExecutionConfig::objectReuseMode, "data.JobExecutionConfig.objectReuseMode"),
        FlinkLocalizedColumn(JobExecutionConfig::userConfig, "data.JobExecutionConfig.userConfig")
      )
    }
  }
}
