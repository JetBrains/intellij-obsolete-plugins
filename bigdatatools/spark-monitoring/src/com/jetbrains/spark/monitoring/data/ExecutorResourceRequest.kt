package com.jetbrains.spark.monitoring.data

data class ExecutorResourceRequest(
  val resourceName: String = "",
  val amount: Long = 0,
  val discoveryScript: String = "",
  val vendor: String = ""
)