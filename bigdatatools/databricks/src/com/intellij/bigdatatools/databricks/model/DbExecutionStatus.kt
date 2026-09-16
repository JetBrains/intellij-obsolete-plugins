package com.intellij.bigdatatools.databricks.model

enum class DbExecutionStatus {
  Queued,
  Running,
  Cancelling,
  Finished,
  Cancelled,
  Error;

}