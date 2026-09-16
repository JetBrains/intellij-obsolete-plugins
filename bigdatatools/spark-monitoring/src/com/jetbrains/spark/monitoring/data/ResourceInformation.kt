package com.jetbrains.spark.monitoring.data

data class ResourceInformation(
  val name: String = "",
  val addresses: List<String> = emptyList()
)