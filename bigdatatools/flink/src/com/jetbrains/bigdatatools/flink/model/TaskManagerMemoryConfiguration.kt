package com.jetbrains.bigdatatools.flink.model

data class TaskManagerMemoryConfiguration(
  val frameworkHeap: Long? = null,
  val frameworkOffHeap: Long? = null,
  val jvmMetaspace: Long? = null,
  val jvmOverhead: Long? = null,
  val managedMemory: Long? = null,
  val networkMemory: Long? = null,
  val taskHeap: Long? = null,
  val taskOffHeap: Long? = null,
  val totalFlinkMemory: Long? = null,
  val totalProcessMemory: Long? = null
)