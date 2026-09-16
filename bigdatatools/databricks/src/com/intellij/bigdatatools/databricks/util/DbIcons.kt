package com.intellij.bigdatatools.databricks.util

import com.databricks.sdk.service.compute.CommandStatus
import com.databricks.sdk.service.compute.State
import com.databricks.sdk.service.jobs.RunLifeCycleState
import com.databricks.sdk.service.jobs.RunResultState
import com.intellij.bigdatatools.databricks.icons.BigdatatoolsDatabricksIcons
import com.intellij.ui.LayeredIcon
import javax.swing.Icon

object DbIcons {
  fun getForCommandStatus(commandStatus: CommandStatus): Icon = when (commandStatus) {
    CommandStatus.CANCELLED -> BigdatatoolsDatabricksIcons.Status.StatusSkipped
    CommandStatus.CANCELLING -> BigdatatoolsDatabricksIcons.Status.StatusSkipped
    CommandStatus.ERROR -> BigdatatoolsDatabricksIcons.Status.StatusError
    CommandStatus.FINISHED -> BigdatatoolsDatabricksIcons.Status.StatusPassed
    CommandStatus.QUEUED -> BigdatatoolsDatabricksIcons.Status.StatusNotRun
    CommandStatus.RUNNING -> CLUSTER_RUNNING
  }

  fun getForRunResultState(runResultState: RunResultState): Icon = when (runResultState) {
    RunResultState.CANCELED -> BigdatatoolsDatabricksIcons.Status.StatusSkipped
    RunResultState.EXCLUDED -> BigdatatoolsDatabricksIcons.Status.StatusSkipped
    RunResultState.FAILED -> BigdatatoolsDatabricksIcons.Status.StatusError
    RunResultState.MAXIMUM_CONCURRENT_RUNS_REACHED -> BigdatatoolsDatabricksIcons.Status.StatusError
    RunResultState.SUCCESS -> BigdatatoolsDatabricksIcons.Status.StatusPassed
    RunResultState.SUCCESS_WITH_FAILURES -> BigdatatoolsDatabricksIcons.Status.StatusPassedIgnored
    RunResultState.TIMEDOUT -> BigdatatoolsDatabricksIcons.Status.StatusSkipped
    RunResultState.UPSTREAM_CANCELED -> BigdatatoolsDatabricksIcons.Status.StatusSkipped
    RunResultState.UPSTREAM_FAILED -> BigdatatoolsDatabricksIcons.Status.StatusSkipped
    RunResultState.DISABLED -> BigdatatoolsDatabricksIcons.Status.StatusSkipped
  }

  fun getForClusterState(state: State?): Icon = when (state) {
    State.ERROR -> CLUSTER_ERROR
    State.PENDING -> CLUSTER_WARNING
    State.RESIZING -> CLUSTER_WARNING
    State.RESTARTING -> CLUSTER_WARNING
    State.RUNNING -> CLUSTER_RUNNING
    State.TERMINATED -> CLUSTER_EMPTY
    State.TERMINATING -> CLUSTER_EMPTY
    State.UNKNOWN -> CLUSTER_ERROR
    null -> CLUSTER_EMPTY
  }

  fun getForRunLifeCycleState(runLifeCycleState: RunLifeCycleState): Icon = when (runLifeCycleState) {
    RunLifeCycleState.BLOCKED -> BigdatatoolsDatabricksIcons.Status.StatusError
    RunLifeCycleState.INTERNAL_ERROR -> BigdatatoolsDatabricksIcons.Status.StatusError
    RunLifeCycleState.PENDING -> BigdatatoolsDatabricksIcons.Status.StatusNotRun
    RunLifeCycleState.QUEUED -> BigdatatoolsDatabricksIcons.Status.StatusNotRun
    RunLifeCycleState.RUNNING -> BigdatatoolsDatabricksIcons.Status.StatusPassed
    RunLifeCycleState.SKIPPED -> BigdatatoolsDatabricksIcons.Status.StatusSkipped
    RunLifeCycleState.TERMINATED -> BigdatatoolsDatabricksIcons.Status.StatusSkipped
    RunLifeCycleState.TERMINATING -> BigdatatoolsDatabricksIcons.Status.StatusSkipped
    RunLifeCycleState.WAITING_FOR_RETRY -> BigdatatoolsDatabricksIcons.Status.StatusNotRun
  }

  val CLUSTER_RUNNING = LayeredIcon.layeredIcon { arrayOf(BigdatatoolsDatabricksIcons.Cluster, BigdatatoolsDatabricksIcons.Running) }
  val CLUSTER_EMPTY = LayeredIcon.layeredIcon { arrayOf(BigdatatoolsDatabricksIcons.Cluster, BigdatatoolsDatabricksIcons.Empty) }
  val CLUSTER_WARNING = LayeredIcon.layeredIcon { arrayOf(BigdatatoolsDatabricksIcons.Cluster, BigdatatoolsDatabricksIcons.Warning) }
  val CLUSTER_ERROR = LayeredIcon.layeredIcon { arrayOf(BigdatatoolsDatabricksIcons.Cluster, BigdatatoolsDatabricksIcons.Error) }
}