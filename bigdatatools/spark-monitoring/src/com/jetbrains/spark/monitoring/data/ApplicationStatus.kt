package com.jetbrains.spark.monitoring.data

import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import org.jetbrains.annotations.Nls

//https://github.com/apache/spark/blob/5264164a67df498b73facae207eda12ee133be7d/core/src/main/java/org/apache/spark/status/api/v1/ApplicationStatus.java
enum class ApplicationStatus(@Nls override val text: String, val importance: Int) : DisplayableStatus {
  COMPLETE(SMMessagesBundle.message("status.complete"), 3),
  RUNNING(SMMessagesBundle.message("status.running"), 1),
  STARTING(SMMessagesBundle.message("status.starting"), 0),
  ERROR(SMMessagesBundle.message("status.error"), 3),
  UNKNOWN(SMMessagesBundle.message("status.unknown"), 1),
  ;
}