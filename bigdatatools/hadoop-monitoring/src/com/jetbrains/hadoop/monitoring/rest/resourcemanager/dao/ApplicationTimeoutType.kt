package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Application timeout type.
 */
enum class ApplicationTimeoutType {
  /**
   *
   *
   * Timeout imposed on overall application life time. It includes actual
   * run-time plus non-runtime. Non-runtime delays are time elapsed by scheduler
   * to allocate container, time taken to store in RMStateStore and etc.
   *
   * If this is set, then timeout monitoring start from application submission
   * time.
   */
  LIFETIME
}