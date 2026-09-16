package com.intellij.bigdatatools.emr.table.renderers

import com.intellij.bigdatatools.emr.rfs.EmrRfsTreeNode
import com.intellij.icons.AllIcons
import com.jetbrains.bigdatatools.common.table.renderers.AbstractIconRenderer
import software.amazon.awssdk.services.emr.model.ClusterState
import javax.swing.Icon

class ClusterStateRenderer : AbstractIconRenderer(
  defaultIcons, AllIcons.RunConfigurations.TestUnknown) {
  companion object {
    @Suppress("UNCHECKED_CAST")
    val defaultIcons = ClusterState.entries.associate { it.name to EmrRfsTreeNode.getIconForCluster(it) } as Map<String, Icon>
  }
}