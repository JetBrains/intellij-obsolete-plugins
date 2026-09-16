package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle
import org.jetbrains.annotations.Nls

enum class YarnApplicationState(@Nls val text: String) {
  /** Application which was just created. */
  NEW(HadoopMessagesBundle.message("yarn.application.state.new")),

  /** Application which is being saved. */
  NEW_SAVING(HadoopMessagesBundle.message("yarn.application.state.new.saving")),

  /** Application which has been submitted. */
  SUBMITTED(HadoopMessagesBundle.message("yarn.application.state.submitted")),

  /** Application has been accepted by the scheduler. */
  ACCEPTED(HadoopMessagesBundle.message("yarn.application.state.accepted")),

  /** Application which is currently running. */
  RUNNING(HadoopMessagesBundle.message("yarn.application.state.running")),

  /**Application which finished successfully. */
  FINISHED(HadoopMessagesBundle.message("yarn.application.state.finished")),

  /** Application which failed. */
  FAILED(HadoopMessagesBundle.message("yarn.application.state.failed")),

  /** Application which was terminated by a user or admin. */
  KILLED(HadoopMessagesBundle.message("yarn.application.state.killed")),
}