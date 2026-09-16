package com.jetbrains.spark.monitoring.data

data class OutputMetricDistributions(
  val bytesWritten: Array<Double> = Array(5) { 0.0 },
  val recordsWritten: Array<Double> = Array(5) { 0.0 }
)