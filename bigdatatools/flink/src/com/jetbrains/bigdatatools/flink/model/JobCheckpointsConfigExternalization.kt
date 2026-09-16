package com.jetbrains.bigdatatools.flink.model

import com.squareup.moshi.Json

data class JobCheckpointsConfigExternalization(
  @Json(name = "delete_on_cancellation")
  val deleteOnCancellation: Boolean,
  val enabled: Boolean
)