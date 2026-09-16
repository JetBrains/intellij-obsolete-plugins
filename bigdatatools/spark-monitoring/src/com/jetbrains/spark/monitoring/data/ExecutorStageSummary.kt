package com.jetbrains.spark.monitoring.data

data class ExecutorStageSummary(
  val taskTime: Long = 0,
  val failedTasks: Int = 0,
  val succeededTasks: Int = 0,
  val killedTasks: Int = 0,
  val inputBytes: Long = 0,
  val inputRecords: Long = 0,
  val outputBytes: Long = 0,
  val outputRecords: Long = 0,
  val shuffleRead: Long = 0,
  val shuffleReadRecords: Long = 0,
  val shuffleWrite: Long = 0,
  val shuffleWriteRecords: Long = 0,
  val memoryBytesSpilled: Long = 0,
  val diskBytesSpilled: Long = 0,
  val isBlacklistedForStage: Boolean = false
)