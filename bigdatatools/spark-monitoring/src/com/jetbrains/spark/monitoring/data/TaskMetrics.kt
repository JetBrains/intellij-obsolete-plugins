package com.jetbrains.spark.monitoring.data

import com.intellij.openapi.util.text.StringUtil
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties

data class TaskMetrics(
  val executorDeserializeTime: Long,
  val executorDeserializeCpuTime: Long,
  val executorRunTime: Long,
  val executorCpuTime: Long,
  val resultSize: Long,
  val jvmGcTime: Long,
  val resultSerializationTime: Long,
  val memoryBytesSpilled: Long,
  val diskBytesSpilled: Long,
  val peakExecutionMemory: Long = -1,
  val inputMetrics: InputMetrics,
  val outputMetrics: OutputMetrics,
  val shuffleReadMetrics: ShuffleReadMetrics,
  val shuffleWriteMetrics: ShuffleWriteMetrics
) {
  companion object {
    val fieldsMap: List<Pair<String, (TaskMetrics) -> Long>> by lazy {

      val result = ArrayList<Pair<String, (TaskMetrics) -> Long>>()

      TaskMetrics::class.declaredMemberProperties.forEach { property ->
        if (property.returnType == Long::class.createType()) {
          result.add(Pair(property.name) { taskMetrics: TaskMetrics -> property.get(taskMetrics) as Long })
        }
      }

      InputMetrics.fieldsMap.forEach {
        result.add(Pair("input${StringUtil.capitalize(it.key)}") { taskMetrics: TaskMetrics -> it.value(taskMetrics.inputMetrics) })
      }

      OutputMetrics.fieldsMap.forEach {
        result.add(Pair("output${StringUtil.capitalize(it.key)}") { taskMetrics: TaskMetrics -> it.value(taskMetrics.outputMetrics) })
      }

      ShuffleReadMetrics.fieldsMap.forEach {
        result.add(
          Pair("shuffleRead${StringUtil.capitalize(it.key)}") { taskMetrics: TaskMetrics -> it.value(taskMetrics.shuffleReadMetrics) })
      }

      ShuffleWriteMetrics.fieldsMap.forEach {
        result.add(
          Pair("shuffleWrite${StringUtil.capitalize(it.key)}") { taskMetrics: TaskMetrics -> it.value(taskMetrics.shuffleWriteMetrics) })
      }

      result
    }
  }
}