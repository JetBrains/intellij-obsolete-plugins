package com.jetbrains.bigdatatools.dataproc.ui.component

import com.google.cloud.dataproc.v1.ClusterStatus
import com.jetbrains.bigdatatools.common.table.renderers.AbstractIconRenderer
import com.jetbrains.bigdatatools.dataproc.rfs.DataprocRfsTreeNode
import java.awt.Component
import javax.swing.JTable

class DataprocClusterStateRenderer : AbstractIconRenderer(emptyMap(), null) {
  @Suppress("HardCodedStringLiteral")
  override fun getTableCellRendererComponent(table: JTable?,
                                             value: Any?,
                                             isSelected: Boolean,
                                             hasFocus: Boolean,
                                             row: Int,
                                             column: Int): Component {
    this.text = ""
    this.toolTipText = ""
    this.icon = null

    val state = value as? ClusterStatus.State ?: return this
    this.toolTipText = state.name
    this.icon = DataprocRfsTreeNode.getIconForCluster(state)
    return this
  }
}