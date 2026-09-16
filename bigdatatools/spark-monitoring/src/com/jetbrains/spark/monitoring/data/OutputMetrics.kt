package com.jetbrains.spark.monitoring.data

import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties

data class OutputMetrics(
  val bytesWritten: Long = 0,
  val recordsWritten: Long = 0
) {
  companion object {
    val fieldsMap: HashMap<String, (OutputMetrics) -> Long> by lazy {
      val result = HashMap<String, (OutputMetrics) -> Long>()

      OutputMetrics::class.declaredMemberProperties.forEach { property ->
        if (property.returnType == Long::class.createType()) {
          result[property.name] = { taskOutputMetrics: OutputMetrics -> property.get(taskOutputMetrics) as Long }
        }
      }

      result
    }
  }
}