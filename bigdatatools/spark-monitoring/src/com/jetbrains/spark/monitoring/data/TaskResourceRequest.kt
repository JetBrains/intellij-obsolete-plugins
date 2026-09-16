package com.jetbrains.spark.monitoring.data

data class TaskResourceRequest(
  val resourceName: String = "",
  val amount: Double = 0.0
)