package com.intellij.bigdatatools.databricks.model

import com.databricks.sdk.service.compute.CommandStatus
import com.databricks.sdk.service.jobs.RunLifeCycleState
import com.intellij.bigdatatools.databricks.icons.BigdatatoolsDatabricksIcons
import com.intellij.ui.AnimatedIcon
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath

val CommandStatus.isRunning get() = this in setOf(CommandStatus.RUNNING, CommandStatus.QUEUED, CommandStatus.CANCELLING)

val RfsPath.withWorkspace get() = "/Workspace" + this.stringRepresentation()

val RunLifeCycleState.isTerminated
  get() = this in setOf(
    RunLifeCycleState.INTERNAL_ERROR,
    RunLifeCycleState.SKIPPED,
    RunLifeCycleState.TERMINATED,
  )

val CommandStatus.icon
  get() = when (this) {
    CommandStatus.CANCELLED -> BigdatatoolsDatabricksIcons.Status.StatusSkipped
    CommandStatus.CANCELLING -> AnimatedIcon.Default()
    CommandStatus.ERROR -> BigdatatoolsDatabricksIcons.Status.StatusFailed
    CommandStatus.FINISHED -> BigdatatoolsDatabricksIcons.Status.StatusPassed
    CommandStatus.QUEUED -> AnimatedIcon.Default()
    CommandStatus.RUNNING -> AnimatedIcon.Default()
  }

val RunLifeCycleState.icon
  get() = when (this) {
    RunLifeCycleState.BLOCKED -> AnimatedIcon.Default()
    RunLifeCycleState.INTERNAL_ERROR -> BigdatatoolsDatabricksIcons.Status.StatusFailed
    RunLifeCycleState.PENDING -> AnimatedIcon.Default()
    RunLifeCycleState.QUEUED -> AnimatedIcon.Default()
    RunLifeCycleState.RUNNING -> AnimatedIcon.Default()
    RunLifeCycleState.SKIPPED -> BigdatatoolsDatabricksIcons.Status.StatusSkipped
    RunLifeCycleState.TERMINATED -> BigdatatoolsDatabricksIcons.Status.StatusPassed
    RunLifeCycleState.TERMINATING -> AnimatedIcon.Default()
    RunLifeCycleState.WAITING_FOR_RETRY -> AnimatedIcon.Default()
  }