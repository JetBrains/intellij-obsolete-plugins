package com.jetbrains.bigdatatools.dataproc.model

import com.google.cloud.dataproc.v1.Cluster
import com.google.cloud.dataproc.v1.ClusterStatus
import com.intellij.bigdatatools.coreUi.util.MessagesBundle

enum class DataprocClusterState(val title: String) {
  RUNNING(MessagesBundle.message("cluster.state.running")),
  TERMINATED(MessagesBundle.message("cluster.state.terminated")),
  TERMINATED_LAST_HOUR(MessagesBundle.message("cluster.state.terminated.last.hour")),
  TERMINATED_LAST_DAY(MessagesBundle.message("cluster.state.terminated.last.day")),
  TERMINATED_LAST_WEEK(MessagesBundle.message("cluster.state.terminated.last.week")),
  FAILED(MessagesBundle.message("cluster.state.failed"));


  fun isSupported(clusterInfo: Cluster): Boolean {
    val finishedTime = clusterInfo.status?.stateStartTime?.nanos?.toLong()?.div(1000)
    val curTime = System.currentTimeMillis()

    val state = clusterInfo.status?.state ?: return false


    return when (this) {
      RUNNING -> state.isActive
      FAILED -> state.isError
      TERMINATED -> state.isTerminated
      TERMINATED_LAST_HOUR -> state.isTerminated && finishedTime != null && curTime - finishedTime < 1000 * 60 * 60
      TERMINATED_LAST_DAY -> state.isTerminated && finishedTime != null && curTime - finishedTime < 24 * 1000 * 60 * 60
      TERMINATED_LAST_WEEK -> state.isTerminated && finishedTime != null && curTime - finishedTime < 7 * 24 * 1000 * 60 * 60
    }
  }

  companion object {
    private val errorStates = listOf(ClusterStatus.State.UNKNOWN, ClusterStatus.State.ERROR, ClusterStatus.State.ERROR_DUE_TO_UPDATE,
                                     ClusterStatus.State.UNRECOGNIZED)
    private val activeStates = listOf(ClusterStatus.State.UNKNOWN, ClusterStatus.State.CREATING, ClusterStatus.State.RUNNING,
                                      ClusterStatus.State.DELETING,
                                      ClusterStatus.State.UPDATING, ClusterStatus.State.STOPPING, ClusterStatus.State.STARTING,
                                      ClusterStatus.State.UNRECOGNIZED)
    private val terminatedStates = listOf(ClusterStatus.State.UNKNOWN, ClusterStatus.State.DELETING, ClusterStatus.State.CREATING,
                                          ClusterStatus.State.STOPPING, ClusterStatus.State.STOPPED,
                                          ClusterStatus.State.STARTING, ClusterStatus.State.UNRECOGNIZED)

    val ClusterStatus.State.isActive: Boolean
      get() = this in activeStates

    val ClusterStatus.State.isError: Boolean
      get() = this in errorStates

    val ClusterStatus.State.isTerminated
      get() = this in terminatedStates

    fun getFor(dataprocClusterInfo: DataprocClusterInfo): DataprocClusterState {
      return values().first { it.isSupported(dataprocClusterInfo.cluster) }
    }
  }
}