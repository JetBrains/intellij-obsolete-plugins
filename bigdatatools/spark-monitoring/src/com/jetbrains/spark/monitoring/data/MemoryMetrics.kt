package com.jetbrains.spark.monitoring.data

data class MemoryMetrics(
  val usedOnHeapStorageMemory: Long,
  val usedOffHeapStorageMemory: Long,
  val totalOnHeapStorageMemory: Long,
  val totalOffHeapStorageMemory: Long
)