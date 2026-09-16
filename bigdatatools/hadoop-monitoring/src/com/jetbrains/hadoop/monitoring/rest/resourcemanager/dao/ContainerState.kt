package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

enum class ContainerState {
  /** New container */
  NEW,

  /** Running container */
  RUNNING,

  /** Completed container */
  COMPLETE
}