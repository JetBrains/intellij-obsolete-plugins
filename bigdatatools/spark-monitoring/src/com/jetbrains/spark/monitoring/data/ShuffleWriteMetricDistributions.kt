package com.jetbrains.spark.monitoring.data

data class ShuffleWriteMetricDistributions(
  val writeBytes: Array<Double> = Array(5) { 0.0 },
  val writeRecords: Array<Double> = Array(5) { 0.0 },
  val writeTime: Array<Double> = Array(5) { 0.0 }
)