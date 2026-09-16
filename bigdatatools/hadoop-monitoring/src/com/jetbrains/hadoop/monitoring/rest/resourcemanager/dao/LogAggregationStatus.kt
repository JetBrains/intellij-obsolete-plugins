package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

enum class LogAggregationStatus {
  /** Log Aggregation is Disabled.  */
  DISABLED,

  /** Log Aggregation does not Start.  */
  NOT_START,

  /** Log Aggregation is Running.  */
  RUNNING,

  /** Log Aggregation is Running, but has failures in previous cycles.  */
  RUNNING_WITH_FAILURE,

  /**
   * Log Aggregation is Succeeded. All of the logs have been aggregated
   * successfully.
   */
  SUCCEEDED,

  /**
   * Log Aggregation is completed. But at least one of the logs have not been
   * aggregated.
   */
  FAILED,

  /**
   * The application is finished, but the log aggregation status is not updated
   * for a long time.
   * @see YarnConfiguration.LOG_AGGREGATION_STATUS_TIME_OUT_MS
   */
  TIME_OUT
}