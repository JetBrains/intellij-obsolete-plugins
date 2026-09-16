package com.jetbrains.spark.monitoring.graph.view

import com.intellij.openapi.graph.GraphManager
import com.intellij.openapi.graph.builder.components.BasicNodesGroup
import com.intellij.openapi.graph.layout.DiscreteNodeLabelModel
import com.intellij.openapi.graph.view.LineType
import com.intellij.openapi.graph.view.ShapeNodePainter
import com.intellij.openapi.graph.view.YLabel
import com.intellij.openapi.graph.view.hierarchy.GroupNodeRealizer
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI

data class DotNodeGroup(private val groupName: String, val parentGroup: DotNodeGroup?) : BasicNodesGroup(groupName, parentGroup) {

  override fun isClosed() = false

  override fun setClosed(closed: Boolean) = Unit

  override fun createGroupNodeRealizer(): GroupNodeRealizer = GraphManager.getGraphManager().createGroupNodeRealizer().apply {
    shapeType = ShapeNodePainter.ROUND_RECT
    isInnerGraphDisplayEnabled = true
    isAutoBoundsEnabled = true
    labelText = groupName
    openGroupIcon = null
    closedGroupIcon = null
    fillColor = null
    lineColor = JBColor.foreground()
    lineType = LineType.LINE_1

    label.apply {
      val labelModel = GraphManager.getGraphManager().createDiscreteNodeLabelModel(DiscreteNodeLabelModel.TOP)
      setLabelModel(labelModel)

      backgroundColor = null
      textColor = JBColor.foreground()
      horizontalTextPosition = YLabel.RIGHT_TEXT_POSITION
      verticalTextPosition = YLabel.CENTER_TEXT_POSITION
      insets = JBUI.insets(5, 7, 5, 10)
    }
  }
}