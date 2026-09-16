package com.jetbrains.bigdatatools.flink.model

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo

data class JobCheckpointsLatest(
  val completed: JobCheckpointsLatestCompleted?,
  val failed: JobCheckpointsLatestFailed?,
  val restored: JobCheckpointsLatestRestored?,
  val savepoint: JobCheckpointsLatestCompleted?
) : RemoteInfo