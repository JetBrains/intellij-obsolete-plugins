package com.jetbrains.spark.monitoring.data

import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties

data class ShuffleReadMetrics(
  val remoteBlocksFetched: Long = 0,
  val localBlocksFetched: Long = 0,
  val fetchWaitTime: Long = 0,
  val remoteBytesRead: Long = 0,
  val localBytesRead: Long = 0,
  val recordsRead: Long = 0
) {
  companion object {
    val fieldsMap: HashMap<String, (ShuffleReadMetrics) -> Long> by lazy {
      val result = HashMap<String, (ShuffleReadMetrics) -> Long>()

      ShuffleReadMetrics::class.declaredMemberProperties.forEach { property ->
        if (property.returnType == Long::class.createType()) {
          result[property.name] = { taskShuffleReadMetrics: ShuffleReadMetrics -> property.get(taskShuffleReadMetrics) as Long }
        }
      }

      result
    }
  }
}