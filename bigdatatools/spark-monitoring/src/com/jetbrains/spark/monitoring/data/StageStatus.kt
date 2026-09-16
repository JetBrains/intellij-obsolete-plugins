package com.jetbrains.spark.monitoring.data

import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import org.jetbrains.annotations.Nls

// https://github.com/apache/spark/blob/5264164a67df498b73facae207eda12ee133be7d/core/src/main/java/org/apache/spark/status/api/v1/StageStatus.java
enum class StageStatus(@Nls override val text: String) : DisplayableStatus {
  ACTIVE(SMMessagesBundle.message("status.active")),
  COMPLETE(SMMessagesBundle.message("status.complete")),
  PENDING(SMMessagesBundle.message("status.pending")),
  FAILED(SMMessagesBundle.message("status.failed")),
  SKIPPED(SMMessagesBundle.message("status.skipped"))
}