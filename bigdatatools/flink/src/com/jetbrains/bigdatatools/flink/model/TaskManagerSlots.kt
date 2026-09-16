package com.jetbrains.bigdatatools.flink.model

data class TaskManagerSlots(
  val jobId: String,
  val resource: TaskManagerTotalResource
)