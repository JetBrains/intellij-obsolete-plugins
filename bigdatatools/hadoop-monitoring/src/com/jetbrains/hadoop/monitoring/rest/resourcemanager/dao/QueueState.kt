package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Enum representing queue state
 */
enum class QueueState(val stateName: String) {
  STOPPED("stopped"), RUNNING("running"), UNDEFINED("undefined");
}