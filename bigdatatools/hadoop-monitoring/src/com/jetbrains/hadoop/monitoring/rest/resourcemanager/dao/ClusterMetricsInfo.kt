package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.hadoop.monitoring.util.HadoopLocalizedField

data class ClusterMetricsInfo(
  val appsSubmitted: Int = 0,
  val appsCompleted: Int = 0,
  val appsPending: Int = 0,
  val appsRunning: Int = 0,
  val appsFailed: Int = 0,
  val appsKilled: Int = 0,
  val reservedMB: Long = 0,
  val availableMB: Long = 0,
  val allocatedMB: Long = 0,
  val reservedVirtualCores: Long = 0,
  val availableVirtualCores: Long = 0,
  val allocatedVirtualCores: Long = 0,
  val containersAllocated: Int = 0,
  val containersReserved: Int = 0,
  val containersPending: Int = 0,
  val totalMB: Long = 0,
  val totalVirtualCores: Long = 0,
  val totalNodes: Int = 0,
  val lostNodes: Int = 0,
  val unhealthyNodes: Int = 0,
  val decommissioningNodes: Int = 0,
  val decommissionedNodes: Int = 0,
  val rebootedNodes: Int = 0,
  val activeNodes: Int = 0,
  val shutdownNodes: Int = 0,

  // Total used resource of the cluster, including all partitions
  val totalUsedResourcesAcrossPartition: ResourceInfo? = null,

  // Total registered resources of the cluster, including all partitions
  val totalClusterResourcesAcrossPartition: ResourceInfo? = null
) : RemoteInfo {
  companion object {
    val renderableColumns: List<HadoopLocalizedField<ClusterMetricsInfo>> by lazy {
      listOf(
        HadoopLocalizedField(ClusterMetricsInfo::appsSubmitted, "data.ClusterMetricsInfo.appsSubmitted"),
        HadoopLocalizedField(ClusterMetricsInfo::appsCompleted, "data.ClusterMetricsInfo.appsCompleted"),
        HadoopLocalizedField(ClusterMetricsInfo::appsPending, "data.ClusterMetricsInfo.appsPending"),
        HadoopLocalizedField(ClusterMetricsInfo::appsRunning, "data.ClusterMetricsInfo.appsRunning"),
        HadoopLocalizedField(ClusterMetricsInfo::appsFailed, "data.ClusterMetricsInfo.appsFailed"),
        HadoopLocalizedField(ClusterMetricsInfo::appsKilled, "data.ClusterMetricsInfo.appsKilled"),
        HadoopLocalizedField(ClusterMetricsInfo::reservedMB, "data.ClusterMetricsInfo.reservedMB"),
        HadoopLocalizedField(ClusterMetricsInfo::availableMB, "data.ClusterMetricsInfo.availableMB"),
        HadoopLocalizedField(ClusterMetricsInfo::allocatedMB, "data.ClusterMetricsInfo.allocatedMB"),
        HadoopLocalizedField(ClusterMetricsInfo::reservedVirtualCores, "data.ClusterMetricsInfo.reservedVirtualCores"),
        HadoopLocalizedField(ClusterMetricsInfo::availableVirtualCores, "data.ClusterMetricsInfo.availableVirtualCores"),
        HadoopLocalizedField(ClusterMetricsInfo::allocatedVirtualCores, "data.ClusterMetricsInfo.allocatedVirtualCores"),
        HadoopLocalizedField(ClusterMetricsInfo::containersAllocated, "data.ClusterMetricsInfo.containersAllocated"),
        HadoopLocalizedField(ClusterMetricsInfo::containersReserved, "data.ClusterMetricsInfo.containersReserved"),
        HadoopLocalizedField(ClusterMetricsInfo::containersPending, "data.ClusterMetricsInfo.containersPending"),
        HadoopLocalizedField(ClusterMetricsInfo::totalMB, "data.ClusterMetricsInfo.totalMB"),
        HadoopLocalizedField(ClusterMetricsInfo::totalVirtualCores, "data.ClusterMetricsInfo.totalVirtualCores"),
        HadoopLocalizedField(ClusterMetricsInfo::totalNodes, "data.ClusterMetricsInfo.totalNodes"),
        HadoopLocalizedField(ClusterMetricsInfo::lostNodes, "data.ClusterMetricsInfo.lostNodes"),
        HadoopLocalizedField(ClusterMetricsInfo::unhealthyNodes, "data.ClusterMetricsInfo.unhealthyNodes"),
        HadoopLocalizedField(ClusterMetricsInfo::decommissioningNodes, "data.ClusterMetricsInfo.decommissioningNodes"),
        HadoopLocalizedField(ClusterMetricsInfo::decommissionedNodes, "data.ClusterMetricsInfo.decommissionedNodes"),
        HadoopLocalizedField(ClusterMetricsInfo::rebootedNodes, "data.ClusterMetricsInfo.rebootedNodes"),
        HadoopLocalizedField(ClusterMetricsInfo::activeNodes, "data.ClusterMetricsInfo.activeNodes"),
        HadoopLocalizedField(ClusterMetricsInfo::shutdownNodes, "data.ClusterMetricsInfo.shutdownNodes"),
        HadoopLocalizedField(ClusterMetricsInfo::totalUsedResourcesAcrossPartition,
                             "data.ClusterMetricsInfo.totalUsedResourcesAcrossPartition"),
        HadoopLocalizedField(ClusterMetricsInfo::totalClusterResourcesAcrossPartition,
                             "data.ClusterMetricsInfo.totalClusterResourcesAcrossPartition")
      )
    }
  }
}