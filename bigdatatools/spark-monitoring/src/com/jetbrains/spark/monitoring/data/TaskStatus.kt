package com.jetbrains.spark.monitoring.data

import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import org.jetbrains.annotations.Nls

// https://github.com/apache/spark/blob/master/core/src/main/scala/org/apache/spark/scheduler/TaskInfo.scala
enum class TaskStatus(@Nls override val text: String) : DisplayableStatus {
  GET_RESULT(SMMessagesBundle.message("status.getResult")),
  RUNNING(SMMessagesBundle.message("status.running")),
  FAILED(SMMessagesBundle.message("status.failed")),
  KILLED(SMMessagesBundle.message("status.killed")),
  SUCCESS(SMMessagesBundle.message("status.success")),
  UNKNOWN(SMMessagesBundle.message("status.unknown"))
}