package com.jetbrains.bigdatatools.flink.model

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.squareup.moshi.Json

data class JobCheckpointsLatestRestored(
  @Json(name = "external_path")
  val externalPath: String,
  val id: Long,
  @Json(name = "is_savepoint")
  val isSavepoint: Boolean,
  @Json(name = "restore_timestamp")
  val restoreTimestamp: Long? = null
) : RemoteInfo