package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

import com.intellij.bigdatatools.coreUi.table.renderers.UnixtimeRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.hadoop.monitoring.util.HadoopLocalizedField

data class NodeInfo(
  val rack: String? = null,
  val state: NodeState? = null,
  val id: String = "",
  val nodeHostName: String? = null,
  val nodeHTTPAddress: String? = null,
  @field:UnixtimeRendering
  val lastHealthUpdate: Long = 0,
  val version: String? = null,
  val healthReport: String? = null,
  val numContainers: Int = 0,
  val usedMemoryMB: Long = 0,
  val availMemoryMB: Long = 0,
  val usedVirtualCores: Long = 0,
  val availableVirtualCores: Long = 0,
  val numRunningOpportContainers: Int = 0,
  val usedMemoryOpportGB: Long = 0,
  val usedVirtualCoresOpport: Long = 0,
  val numQueuedContainers: Int = 0,
  val nodeLabels: List<String> = emptyList(),
  val allocationTags: AllocationTagsInfo? = null,
  val resourceUtilization: ResourceUtilizationInfo? = null,
  val usedResource: ResourceInfo? = null,
  val availableResource: ResourceInfo? = null,
  val nodeAttributesInfo: NodeAttributesInfo? = null
) : RemoteInfo {
  companion object {
    val renderableColumns: List<HadoopLocalizedField<NodeInfo>> by lazy {
      listOf(
        HadoopLocalizedField(NodeInfo::rack, "data.NodeInfo.rack"),
        HadoopLocalizedField(NodeInfo::state, "data.NodeInfo.state"),
        HadoopLocalizedField(NodeInfo::id, "data.NodeInfo.id"),
        HadoopLocalizedField(NodeInfo::nodeHostName, "data.NodeInfo.nodeHostName"),
        HadoopLocalizedField(NodeInfo::nodeHTTPAddress, "data.NodeInfo.nodeHTTPAddress"),
        HadoopLocalizedField(NodeInfo::lastHealthUpdate, "data.NodeInfo.lastHealthUpdate"),
        HadoopLocalizedField(NodeInfo::version, "data.NodeInfo.version"),
        HadoopLocalizedField(NodeInfo::healthReport, "data.NodeInfo.healthReport"),
        HadoopLocalizedField(NodeInfo::numContainers, "data.NodeInfo.numContainers"),
        HadoopLocalizedField(NodeInfo::usedMemoryMB, "data.NodeInfo.usedMemoryMB"),
        HadoopLocalizedField(NodeInfo::availMemoryMB, "data.NodeInfo.availMemoryMB"),
        HadoopLocalizedField(NodeInfo::usedVirtualCores, "data.NodeInfo.usedVirtualCores"),
        HadoopLocalizedField(NodeInfo::availableVirtualCores, "data.NodeInfo.availableVirtualCores"),
        HadoopLocalizedField(NodeInfo::numRunningOpportContainers, "data.NodeInfo.numRunningOpportContainers"),
        HadoopLocalizedField(NodeInfo::usedMemoryOpportGB, "data.NodeInfo.usedMemoryOpportGB"),
        HadoopLocalizedField(NodeInfo::usedVirtualCoresOpport, "data.NodeInfo.usedVirtualCoresOpport"),
        HadoopLocalizedField(NodeInfo::numQueuedContainers, "data.NodeInfo.numQueuedContainers"),
        HadoopLocalizedField(NodeInfo::nodeLabels, "data.NodeInfo.nodeLabels"),
        HadoopLocalizedField(NodeInfo::allocationTags, "data.NodeInfo.allocationTags"),
        HadoopLocalizedField(NodeInfo::resourceUtilization, "data.NodeInfo.resourceUtilization"),
        HadoopLocalizedField(NodeInfo::usedResource, "data.NodeInfo.usedResource"),
        HadoopLocalizedField(NodeInfo::availableResource, "data.NodeInfo.availableResource"),
        HadoopLocalizedField(NodeInfo::nodeAttributesInfo, "data.NodeInfo.nodeAttributesInfo")
      )
    }
  }
}