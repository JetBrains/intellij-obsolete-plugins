package com.jetbrains.bigdatatools.flink.model

import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering

data class JobExceptionDataWithStackTrace(
  val exceptionName: String?,
  val stacktrace: String?,
  val location: String?,
  val taskName: String?,
  @field:DateRendering(neededAddChecking = true)
  val timestamp: Long?
)