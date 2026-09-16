package com.jetbrains.bigdatatools.flink.model

data class TaskManagerHardware(
  val cpuCores: Long? = null,
  val freeMemory: Long,
  val managedMemory: Long,
  val physicalMemory: Long
)