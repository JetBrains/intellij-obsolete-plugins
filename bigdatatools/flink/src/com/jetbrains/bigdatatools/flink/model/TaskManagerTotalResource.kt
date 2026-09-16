package com.jetbrains.bigdatatools.flink.model

data class TaskManagerTotalResource(
  val cpuCores: Double,
  val extendedResources: TaskManagerExtendedResource?,
  val managedMemory: Long? = null,
  val networkMemory: Long? = null,
  val taskHeapMemory: Long? = null,
  val taskOffHeapMemory: Long? = null
)

