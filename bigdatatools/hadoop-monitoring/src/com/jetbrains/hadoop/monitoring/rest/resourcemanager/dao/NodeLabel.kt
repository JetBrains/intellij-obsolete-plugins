package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.hadoop.monitoring.util.HadoopLocalizedField

data class NodeLabel(val labelName: String,
                     val labelType: String,
                     val numActiveNodeMangers: Int,
                     val totalResource: ResourceInfo) : RemoteInfo {
  companion object {
    val renderableColumns: List<HadoopLocalizedField<NodeLabel>> by lazy {
      listOf(
        HadoopLocalizedField(NodeLabel::labelName, "data.NodeLabel.labelName"),
        HadoopLocalizedField(NodeLabel::labelType, "data.NodeLabel.labelType"),
        HadoopLocalizedField(NodeLabel::numActiveNodeMangers, "data.NodeLabel.numActiveNodeManagers"),
        HadoopLocalizedField(NodeLabel::totalResource, "data.NodeLabel.totalResource")
      )
    }
  }
}