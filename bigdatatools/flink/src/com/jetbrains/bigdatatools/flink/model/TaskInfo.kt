package com.jetbrains.bigdatatools.flink.model

import java.util.Locale
import kotlin.reflect.full.declaredMemberProperties

data class TaskInfo(
  val total: Int = 0,
  val created: Int = 0,
  val scheduled: Int = 0,
  val deploying: Int = 0,
  val running: Int = 0,
  val finished: Int = 0,
  val canceling: Int = 0,
  val canceled: Int = 0,
  val failed: Int = 0,
  val reconciling: Int = 0,
  val initializing: Int = 0
) {
  override fun toString(): String {
    val totalString = StringBuilder()
    val declaredProperties = TaskInfo::class.declaredMemberProperties
    for (property in declaredProperties) {
      val value = property.get(this)
      if (value != 0) {
        totalString.append("${property.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}:$value ")
      }
    }
    return totalString.toString()
  }
}