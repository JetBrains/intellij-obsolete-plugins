package com.jetbrains.spark.monitoring.data

import java.util.Date

data class ApplicationAttemptInfo(
  val attemptId: String? = "",
  val startTime: Date? = null,
  val endTime: Date? = null,
  val lastUpdated: Date? = null,
  val duration: Long? = 0L,
  val sparkUser: String = "",
  val completed: Boolean = false,
  val lastUpdatedEpoch: Long = 0L,
  val startTimeEpoch: Long = 0L,
  val endTimeEpoch: Long = 0L,
  val appSparkVersion: String = ""
)