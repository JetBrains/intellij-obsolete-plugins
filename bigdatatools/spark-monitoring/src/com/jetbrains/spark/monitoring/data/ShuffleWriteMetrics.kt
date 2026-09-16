package com.jetbrains.spark.monitoring.data

import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties

data class ShuffleWriteMetrics(
  val bytesWritten: Long,
  val writeTime: Long,
  val recordsWritten: Long
) {
  companion object {
    val fieldsMap: HashMap<String, (ShuffleWriteMetrics) -> Long> by lazy {
      val result = HashMap<String, (ShuffleWriteMetrics) -> Long>()

      ShuffleWriteMetrics::class.declaredMemberProperties.forEach { property ->
        if (property.returnType == Long::class.createType()) {
          result[property.name] = { taskShuffleWriteMetrics: ShuffleWriteMetrics -> property.get(taskShuffleWriteMetrics) as Long }
        }
      }

      result
    }
  }
}