package com.intellij.bigdatatools.emr.model

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import software.amazon.awssdk.services.emr.model.ClusterState
import software.amazon.awssdk.services.emr.model.ClusterSummary

enum class EmrClusterState(val title: String) {
  @Deprecated("Not used, Not removed just keep serialization settings. BDIDE-3663")
  STARTING(MessagesBundle.message("cluster.state.starting")),
  RUNNING(MessagesBundle.message("cluster.state.running")),
  TERMINATED(MessagesBundle.message("cluster.state.terminated")),
  TERMINATED_LAST_HOUR(MessagesBundle.message("cluster.state.terminated.last.hour")),
  TERMINATED_LAST_DAY(MessagesBundle.message("cluster.state.terminated.last.day")),
  TERMINATED_LAST_WEEK(MessagesBundle.message("cluster.state.terminated.last.week")),
  FAILED(MessagesBundle.message("cluster.state.failed"));


  fun isSupported(clusterInfo: ClusterSummary): Boolean {
    val finishedTime = clusterInfo.status()?.timeline()?.endDateTime()?.toEpochMilli()
    val curTime = System.currentTimeMillis()

    val state = clusterInfo.status()?.state()
    val isTerminated = state in setOf(ClusterState.TERMINATED, ClusterState.TERMINATED_WITH_ERRORS)

    @Suppress("DEPRECATION")
    return when (this) {
      RUNNING -> state in setOf(ClusterState.BOOTSTRAPPING, ClusterState.STARTING, ClusterState.RUNNING, ClusterState.WAITING,
                                ClusterState.TERMINATING)
      TERMINATED -> isTerminated
      FAILED -> state == ClusterState.TERMINATED_WITH_ERRORS
      TERMINATED_LAST_HOUR -> isTerminated && finishedTime != null && curTime - finishedTime < 1000 * 60 * 60
      TERMINATED_LAST_DAY -> isTerminated && finishedTime != null && curTime - finishedTime < 24 * 1000 * 60 * 60
      TERMINATED_LAST_WEEK -> isTerminated && finishedTime != null && curTime - finishedTime < 7 * 24 * 1000 * 60 * 60
      STARTING -> false
    }
  }

  @Suppress("DEPRECATION")
  fun toOriginStates() = when (this) {
    RUNNING -> setOf(ClusterState.BOOTSTRAPPING,
                     ClusterState.STARTING,
                     ClusterState.RUNNING,
                     ClusterState.WAITING,
                     ClusterState.TERMINATING)
    TERMINATED, TERMINATED_LAST_HOUR, TERMINATED_LAST_DAY, TERMINATED_LAST_WEEK -> setOf(ClusterState.TERMINATED,
                                                                                         ClusterState.TERMINATED_WITH_ERRORS)
    FAILED -> setOf(ClusterState.TERMINATED_WITH_ERRORS)
    STARTING -> setOf()
  }

  companion object {
    val supportedValues = arrayOf(RUNNING, TERMINATED, TERMINATED_LAST_HOUR, TERMINATED_LAST_DAY, TERMINATED_LAST_WEEK, FAILED)
  }
}