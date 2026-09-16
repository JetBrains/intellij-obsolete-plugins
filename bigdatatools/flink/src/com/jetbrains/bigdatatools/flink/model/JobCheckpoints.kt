package com.jetbrains.bigdatatools.flink.model

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo

data class JobCheckpoints(
  val counts: JobCheckpointsCounts,
  val summary: JobCheckpointsSummary,
  val latest: JobCheckpointsLatest,
  val history: List<JobCheckpointsHistory> = emptyList()
) : RemoteInfo