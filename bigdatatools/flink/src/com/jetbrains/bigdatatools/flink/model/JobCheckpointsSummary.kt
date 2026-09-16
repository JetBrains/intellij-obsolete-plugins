package com.jetbrains.bigdatatools.flink.model

import com.squareup.moshi.Json

data class JobCheckpointsSummary(
  @Json(name = "alignment_buffered")
  val alignmentBuffered: JobCheckpointsSummaryStatistics,
  @Json(name = "end_to_end_duration")
  val endToEndDuration: JobCheckpointsSummaryStatistics,
  @Json(name = "persisted_data")
  val persistedData: JobCheckpointsSummaryStatistics,
  @Json(name = "processed_data")
  val processedData: JobCheckpointsSummaryStatistics,
  @Json(name = "state_size")
  val stateSize: JobCheckpointsSummaryStatistics
)