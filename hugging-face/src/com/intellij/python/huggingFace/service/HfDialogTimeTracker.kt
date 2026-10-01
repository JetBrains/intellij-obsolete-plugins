package com.intellij.python.huggingFace.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class HfDialogTimeTracker {
  // may be used for Cache Manager as well
  private var dialogStartTime: Long = 0

  fun startTimer() {
    dialogStartTime = System.currentTimeMillis()
  }

  fun stopAndReportTimer(): Long {
    if (dialogStartTime == 0L) return 0L
    val duration = System.currentTimeMillis() - dialogStartTime
    dialogStartTime = 0L // Reset timer
    return duration
  }

  companion object {
    fun getInstance(project: Project): HfDialogTimeTracker {
      return project.getService(HfDialogTimeTracker::class.java)
    }
  }
}
