package com.jetbrains.bigdatatools.flink.model

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.squareup.moshi.Json

data class JobCheckpointsCounts(
  val completed: Long? = null,
  val failed: Long? = null,
  @Json(name = "in_progress")
  val inProgress: Long? = null,
  val restored: Long? = null,
  val total: Long? = null
) : RemoteInfo
