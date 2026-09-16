package com.jetbrains.spark.monitoring.ui.utils

import com.intellij.icons.AllIcons
import com.intellij.ui.AnimatedIcon
import com.jetbrains.spark.monitoring.data.ApplicationStatus
import com.jetbrains.spark.monitoring.data.JobExecutionStatus
import com.jetbrains.spark.monitoring.data.SqlInfoStatus
import com.jetbrains.spark.monitoring.data.StageStatus
import com.jetbrains.spark.monitoring.data.TaskStatus
import javax.swing.Icon

object IconUtils {

  fun getIconForSqlStatus(status: SqlInfoStatus): Icon {
    return when (status) {
      SqlInfoStatus.COMPLETED -> AllIcons.RunConfigurations.TestPassed
      SqlInfoStatus.RUNNING -> AllIcons.RunConfigurations.TestState.Run_run
      SqlInfoStatus.FAILED -> AllIcons.RunConfigurations.TestFailed
    }
  }

  fun getIconForApplicationStatus(status: ApplicationStatus): Icon? {
    return when (status) {
      ApplicationStatus.COMPLETE -> AllIcons.RunConfigurations.ToolbarPassed
      ApplicationStatus.RUNNING -> AllIcons.RunConfigurations.TestState.Run_run
      ApplicationStatus.STARTING -> AnimatedIcon.Default()
      ApplicationStatus.ERROR -> AllIcons.General.Error
      ApplicationStatus.UNKNOWN -> null
    }
  }

  fun getIconForJobStatus(status: JobExecutionStatus): Icon {
    return when (status) {
      JobExecutionStatus.SUCCEEDED -> AllIcons.RunConfigurations.TestPassed
      JobExecutionStatus.RUNNING -> AllIcons.RunConfigurations.TestState.Run_run
      JobExecutionStatus.FAILED -> AllIcons.RunConfigurations.TestFailed
      JobExecutionStatus.UNKNOWN -> AllIcons.RunConfigurations.TestUnknown
    }
  }

  fun getIconForStageStatus(status: StageStatus): Icon {
    return when (status) {
      StageStatus.COMPLETE -> AllIcons.RunConfigurations.TestPassed
      StageStatus.ACTIVE -> AllIcons.RunConfigurations.TestState.Run_run
      StageStatus.SKIPPED -> AllIcons.RunConfigurations.TestSkipped
      StageStatus.PENDING -> AllIcons.RunConfigurations.TestUnknown
      StageStatus.FAILED -> AllIcons.RunConfigurations.TestFailed
    }
  }

  @Suppress("unused")
  fun getIconForTaskStatus(status: TaskStatus): Icon {
    return when (status) {
      TaskStatus.SUCCESS -> AllIcons.RunConfigurations.TestPassed
      TaskStatus.FAILED -> AllIcons.RunConfigurations.TestFailed
      TaskStatus.KILLED -> AllIcons.RunConfigurations.ToolbarSkipped
      TaskStatus.UNKNOWN -> AllIcons.RunConfigurations.TestUnknown
      TaskStatus.RUNNING -> AllIcons.RunConfigurations.TestState.Run
      TaskStatus.GET_RESULT -> AllIcons.RunConfigurations.TestState.Run
    }
  }
}