package com.jetbrains.bigdatatools.dataproc.model

import com.google.cloud.dataproc.v1.Job
import com.google.cloud.dataproc.v1.JobStatus
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle

enum class DataprocJobState(val title: String) {
  ACTIVE(DataprocMessagesBundle.message("job.state.active")),
  FAILED(DataprocMessagesBundle.message("job.state.failed")),
  CANCELED(DataprocMessagesBundle.message("job.state.canceled")),
  DONE(DataprocMessagesBundle.message("job.state.done"));


  fun isSupported(job: Job) = job.state == this

  companion object {
    val Job.state
      get() = when (status.state) {
        JobStatus.State.STATE_UNSPECIFIED -> FAILED
        JobStatus.State.PENDING -> ACTIVE
        JobStatus.State.SETUP_DONE -> ACTIVE
        JobStatus.State.RUNNING -> ACTIVE
        JobStatus.State.CANCEL_PENDING -> ACTIVE
        JobStatus.State.CANCEL_STARTED -> ACTIVE
        JobStatus.State.CANCELLED -> CANCELED
        JobStatus.State.DONE -> DONE
        JobStatus.State.ERROR -> FAILED
        JobStatus.State.ATTEMPT_FAILURE -> ACTIVE
        JobStatus.State.UNRECOGNIZED -> FAILED
        else -> FAILED
      }
  }
}