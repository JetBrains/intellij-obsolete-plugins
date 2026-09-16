package com.jetbrains.spark.monitoring.data

// https://github.com/apache/spark/blob/5264164a67df498b73facae207eda12ee133be7d/core/src/main/scala/org/apache/spark/scheduler/TaskLocality.scala
enum class TaskLocality {
  PROCESS_LOCAL,
  NODE_LOCAL,
  NO_PREF,
  RACK_LOCAL,
  ANY
}