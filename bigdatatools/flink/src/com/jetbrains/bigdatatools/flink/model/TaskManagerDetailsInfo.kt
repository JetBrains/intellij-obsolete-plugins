package com.jetbrains.bigdatatools.flink.model

data class TaskManagerDetailsInfo(
  val allocatedSlots: List<TaskManagerSlots> = emptyList(),
  val dataPort: Long? = null,
  val freeResource: TaskManagerTotalResource,
  val freeSlots: Long? = null,
  val hardware: TaskManagerHardware,
  val id: String,
  val jmxPort: Long? = null,
  val memoryConfiguration: TaskManagerMemoryConfiguration,
  val metrics: TaskManagerMetrics,
  val path: String,
  val slotsNumber: Long? = null,
  val timeSinceLastHeartbeat: Long? = null,
  val totalResource: TaskManagerTotalResource
)