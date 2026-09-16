package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

enum class FinalApplicationStatus {
  /** Undefined state when either the application has not yet finished  */
  UNDEFINED,

  /** Application which finished successfully.  */
  SUCCEEDED,

  /** Application which failed.  */
  FAILED,

  /** Application which was terminated by a user or admin.  */
  KILLED,

  /** Application which has subtasks with multiple end states.  */
  ENDED
}