package com.jetbrains.spark.monitoring.data

data class InputMetricDistributions(
  val bytesRead: Array<Double> = Array(5) { 0.0 },
  val recordsRead: Array<Double> = Array(5) { 0.0 }
)