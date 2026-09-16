package com.intellij.bigdatatools.databricks.model

import com.databricks.sdk.service.compute.ClusterDetails
import com.databricks.sdk.service.compute.ClusterSource
import com.databricks.sdk.service.compute.State
import com.intellij.openapi.util.NlsSafe
import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import org.jetbrains.annotations.Nls

data class ClusterInfoPresentable(
  @field:NoRendering
  @Transient
  val info: ClusterDetails,
  @Nls @field:NoRendering
  val id: String = info.clusterId,
  @NlsSafe val name: String = info.clusterName,
  val source: ClusterSource = info.clusterSource,
  val nodes: String = info.numWorkers?.toString() ?: info.autoscale?.toString() ?: "",
  val runtime: String = info.sparkVersion,
  val driver: String = info.driverNodeTypeId,
  val creator: String = info.creatorUserName,
  val state: State = info.state
) : RemoteInfo {
  fun isClusterStarted() = state in setOf(State.RUNNING, State.PENDING, State.RESIZING, State.RESTARTING, State.TERMINATING)
  fun isClusterReady() = state == State.RUNNING
  fun isClusterStopped() = state in setOf(State.ERROR, State.TERMINATED, State.TERMINATING)

  companion object {
    fun createFrom(clusterInfo: ClusterDetails): ClusterInfoPresentable = ClusterInfoPresentable(clusterInfo)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ClusterInfoPresentable

    if (id != other.id) return false
    if (state != other.state) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + state.hashCode()
    return result
  }
}