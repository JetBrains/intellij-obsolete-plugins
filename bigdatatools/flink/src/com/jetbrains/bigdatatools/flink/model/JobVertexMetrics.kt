package com.jetbrains.bigdatatools.flink.model

import com.squareup.moshi.Json

data class JobVertexMetrics(
  @Json(name = "read-bytes")
  val bytesReceived: Long,
  @Json(name = "read-bytes-complete")
  val readBytesComplete: Boolean,
  @Json(name = "read-records")
  val recordsReceived: Long? = null,
  @Json(name = "read-records-complete")
  val readRecordsComplete: Boolean,
  @Json(name = "write-bytes")
  val bytesSent: Long,
  @Json(name = "write-bytes-complete")
  val writeBytesComplete: Boolean,
  @Json(name = "write-records")
  val recordsSent: Long? = null,
  @Json(name = "write-records-complete")
  val writeRecordsComplete: Boolean
)