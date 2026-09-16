package com.jetbrains.bigdatatools.flink.model

import com.intellij.openapi.util.NlsSafe

enum class JobExecutionStatus(@NlsSafe val title: String) {
  INITIALIZING("Initializing"),
  CREATED("Created"),
  RUNNING("Running"),
  FAILING("Failing"),
  FAILED("Failed"),
  CANCELLING("Cancelling"),
  CANCELED("Canceled"),
  FINISHED("Finished"),
  RESTARTING("Restarting"),
  SUSPENDED("Suspended"),
  RECONCILING("Reconciling")
}