package com.jetbrains.bigdatatools.flink.model

data class TaskManagerGarbageCollectors(
  val count: Long? = null,
  val name: String,
  val time: Long? = null
)