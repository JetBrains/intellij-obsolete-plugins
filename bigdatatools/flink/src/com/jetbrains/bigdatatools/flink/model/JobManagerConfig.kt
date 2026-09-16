package com.jetbrains.bigdatatools.flink.model

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.flink.util.FlinkLocalizedColumn

data class JobManagerConfig(
  val key: String,
  val value: String
) : RemoteInfo {
  companion object {
    val renderableColumns: List<FlinkLocalizedColumn<JobManagerConfig>> by lazy {
      listOf(
        FlinkLocalizedColumn(JobManagerConfig::key, "data.JobManagerConfig.key"),
        FlinkLocalizedColumn(JobManagerConfig::value, "data.JobManagerConfig.value")
      )
    }
  }
}