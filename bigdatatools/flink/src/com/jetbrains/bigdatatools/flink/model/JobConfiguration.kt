package com.jetbrains.bigdatatools.flink.model

import com.squareup.moshi.Json

data class JobConfiguration(
  val jid: String,
  val name: String,
  @Json(name = "execution-config")
  val executionConfig: JobExecutionConfig
)

