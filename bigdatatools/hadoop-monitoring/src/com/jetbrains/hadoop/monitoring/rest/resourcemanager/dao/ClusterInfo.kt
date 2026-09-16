package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

import com.intellij.bigdatatools.coreUi.table.renderers.UnixtimeRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.hadoop.monitoring.util.HadoopLocalizedField

data class ClusterInfo(
  val id: Long = 0,
  @field:UnixtimeRendering
  val startedOn: Long = 0,
  val state: Service.STATE? = null,
  val haState: HAServiceProtocol.HAServiceState? = null,
  val rmStateStoreName: String? = null,
  val resourceManagerVersion: String? = null,
  val resourceManagerBuildVersion: String? = null,
  val resourceManagerVersionBuiltOn: String? = null,
  val hadoopVersion: String? = null,
  val hadoopBuildVersion: String? = null,
  val hadoopVersionBuiltOn: String? = null,
  val haZooKeeperConnectionState: String? = null
) : RemoteInfo {
  companion object {
    val renderableColumns: List<HadoopLocalizedField<ClusterInfo>> by lazy {
      listOf(
        HadoopLocalizedField(ClusterInfo::id, "data.ClusterInfo.id"),
        HadoopLocalizedField(ClusterInfo::startedOn, "data.ClusterInfo.startedOn"),
        HadoopLocalizedField(ClusterInfo::state, "data.ClusterInfo.state"),
        HadoopLocalizedField(ClusterInfo::haState, "data.ClusterInfo.haState"),
        HadoopLocalizedField(ClusterInfo::rmStateStoreName, "data.ClusterInfo.rmStateStoreName"),
        HadoopLocalizedField(ClusterInfo::resourceManagerVersion, "data.ClusterInfo.resourceManagerVersion"),
        HadoopLocalizedField(ClusterInfo::resourceManagerBuildVersion, "data.ClusterInfo.resourceManagerBuildVersion"),
        HadoopLocalizedField(ClusterInfo::resourceManagerVersionBuiltOn, "data.ClusterInfo.resourceManagerVersionBuiltOn"),
        HadoopLocalizedField(ClusterInfo::hadoopVersion, "data.ClusterInfo.hadoopVersion"),
        HadoopLocalizedField(ClusterInfo::hadoopBuildVersion, "data.ClusterInfo.hadoopBuildVersion"),
        HadoopLocalizedField(ClusterInfo::hadoopVersionBuiltOn, "data.ClusterInfo.hadoopVersionBuiltOn"),
        HadoopLocalizedField(ClusterInfo::haZooKeeperConnectionState, "data.ClusterInfo.haZooKeeperConnectionState")
      )
    }
  }
}