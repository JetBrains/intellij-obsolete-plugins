package com.intellij.bigdatatools.databricks.sync

import com.intellij.bigdatatools.databricks.util.DatabricksBundle

data class DatabricksCliSyncEvent(val type: String,
                                  val action: String?,
                                  val progress: Double?,
                                  val path: String?,
                                  val put: List<String> = emptyList(),
                                  val delete: List<String> = emptyList()) {
  fun getStatus(): SyncStatus {
    return when (type) {
      "start" -> SyncStatus.IN_PROGRESS
      "progress" -> SyncStatus.IN_PROGRESS
      "complete" -> SyncStatus.WATCHING_FOR_CHANGES
      else -> error("Unknown status $type")
    }
  }

  fun getStatusMessage() = when (type) {
    "start" -> DatabricksBundle.message("sync.status.start", put.size + delete.size)
    "progress" -> {
      if (progress == 1.0) {
        when (this.action) {
          "put" -> DatabricksBundle.message("sync.status.upload", path ?: "")
          "delete" -> DatabricksBundle.message("sync.status.delete", path ?: "")
          else -> ""
        }
      }
      else {
        DatabricksBundle.message("sync.status.processing", path ?: "")
      }
    }
    "complete" -> DatabricksBundle.message("sync.status.waiting")
    else -> error("Unknown status $type")
  }
}
