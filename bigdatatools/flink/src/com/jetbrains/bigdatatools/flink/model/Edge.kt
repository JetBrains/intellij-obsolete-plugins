package com.jetbrains.bigdatatools.flink.model

import com.squareup.moshi.Json

data class Edge(
  val num: Long,
  val id: String,
  @Json(name = "ship_strategy") @field:Json(name = "ship_strategy") val shipStrategy: String = "",
  val exchange: String = ""
)