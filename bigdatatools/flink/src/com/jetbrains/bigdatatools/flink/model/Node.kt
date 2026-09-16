package com.jetbrains.bigdatatools.flink.model

import com.squareup.moshi.Json
import org.jetbrains.annotations.Nls

data class Node(
  val id: String,
  val parallelism: Long,
  val operator: String,
  @Json(name = "operator_strategy") val operatorStrategy: String,
  @Nls val description: String,
  val inputs: List<Edge> = emptyList(),
  @Json(name = "optimizer_properties") val optimizerProperties: Any? = null
) {
  val nodeLabel: String = "<html>${description} <br><br>Parallelism: $parallelism</html>"
}