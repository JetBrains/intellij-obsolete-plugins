package com.jetbrains.bigdatatools.flink.model

data class TaskManagerMetrics(
  val directCount: Long? = null,
  val directMax: Long? = null,
  val directUsed: Long? = null,
  val garbageCollectors: List<TaskManagerGarbageCollectors> = emptyList(),
  val heapCommitted: Long? = null,
  val heapMax: Long? = null,
  val heapUsed: Long? = null,
  val mappedCount: Long? = null,
  val mappedMax: Long? = null,
  val mappedUsed: Long? = null,
  val memorySegmentsAvailable: Long? = null,
  val memorySegmentsTotal: Long? = null,
  val nettyShuffleMemoryAvailable: Long? = null,
  val nettyShuffleMemorySegmentsAvailable: Long? = null,
  val nettyShuffleMemorySegmentsTotal: Long? = null,
  val type: Long? = null,
  val nettyShuffleMemorySegmentsUsed: Long? = null,
  val nettyShuffleMemoryTotal: Long? = null,
  val nettyShuffleMemoryUsed: Long? = null,
  val nonHeapCommitted: Long? = null,
  val nonHeapMax: Long? = null,
  val nonHeapUsed: Long? = null
)