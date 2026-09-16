package com.jetbrains.spark.monitoring.data

import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties

data class InputMetrics(
  val bytesRead: Long = 0,
  val recordsRead: Long = 0
) {
  companion object {
    val fieldsMap: HashMap<String, (InputMetrics) -> Long> by lazy {
      val result = HashMap<String, (InputMetrics) -> Long>()

      InputMetrics::class.declaredMemberProperties.forEach { property ->
        if (property.returnType == Long::class.createType()) {
          result[property.name] = { taskInputMetrics: InputMetrics -> property.get(taskInputMetrics) as Long }
        }
      }

      result
    }
  }
}