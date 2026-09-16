package com.jetbrains.spark.monitoring.data

data class ShuffleReadMetricDistributions(
  val readBytes: Array<Double> = Array(5) { 0.0 },
  val readRecords: Array<Double> = Array(5) { 0.0 },
  val remoteBlocksFetched: Array<Double> = Array(5) { 0.0 },
  val localBlocksFetched: Array<Double> = Array(5) { 0.0 },
  val fetchWaitTime: Array<Double> = Array(5) { 0.0 },
  val remoteBytesRead: Array<Double> = Array(5) { 0.0 },
  val remoteBytesReadToDisk: Array<Double> = Array(5) { 0.0 },
  val totalBlocksFetched: Array<Double> = Array(5) { 0.0 }
)