package com.jetbrains.bigdatatools.flink.model

import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.squareup.moshi.Json

data class JobException(
  @Json(name = "all-exceptions")
  val allException: List<JobExceptionType> = emptyList(),
  val exceptionHistory: JobExceptionHistory,
  @Json(name = "root-exception")
  val rootException: String?,
  @field:DateRendering(neededAddChecking = true)
  val timestamp: Long?,
  val truncated: Boolean?
)