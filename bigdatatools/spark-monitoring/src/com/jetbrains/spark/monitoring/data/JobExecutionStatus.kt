package com.jetbrains.spark.monitoring.data

import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import org.jetbrains.annotations.Nls

// https://github.com/apache/spark/blob/5264164a67df498b73facae207eda12ee133be7d/core/src/main/java/org/apache/spark/JobExecutionStatus.java
enum class JobExecutionStatus(@Nls override val text: String) : DisplayableStatus {
  RUNNING(SMMessagesBundle.message("status.running")),
  SUCCEEDED(SMMessagesBundle.message("status.succeeded")),
  FAILED(SMMessagesBundle.message("status.failed")),
  UNKNOWN(SMMessagesBundle.message("status.unknown"))
}
