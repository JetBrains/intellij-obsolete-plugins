package com.jetbrains.bigdatatools.flink.model

import com.squareup.moshi.Json

data class JobDetailsInfo(
  val duration: Long,
  @Json(name = "end-time")
  val endTime: Long,
  val isStoppable: Boolean,
  val jid: String,
  val maxParallelism: Long? = null,
  val name: String,
  val now: Long,
  val plan: Plan,
  @Json(name = "start-time")
  val startTime: Long,
  val state: JobExecutionStatus,
  @Json(name = "status-counts")
  val statusCounts: Any?,
  val timestamps: Any?,
  val vertices: List<JobVertex> = emptyList()
)