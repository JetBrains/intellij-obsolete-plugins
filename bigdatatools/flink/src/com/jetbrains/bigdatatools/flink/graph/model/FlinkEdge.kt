package com.jetbrains.bigdatatools.flink.graph.model

import com.intellij.diagram.DiagramEdgeBase
import com.intellij.diagram.DiagramRelationshipInfo
import com.intellij.diagram.DiagramRelationshipInfoAdapter
import com.intellij.diagram.presentation.DiagramLineType
import com.jetbrains.bigdatatools.flink.model.Node
import org.jetbrains.annotations.NonNls

class FlinkEdge(source: FlinkNode,
                target: FlinkNode,
                @NonNls edgeLabel: String) : DiagramEdgeBase<Node>(source, target, getFlinkRelationship(edgeLabel)) {
  companion object {
    fun getFlinkRelationship(edgeLabel: String): DiagramRelationshipInfoAdapter = DiagramRelationshipInfoAdapter.Builder()
      .setLineType(DiagramLineType.SOLID)
      .setTargetArrow(DiagramRelationshipInfo.STANDARD)
      .setUpperCenterLabel(edgeLabel)
      .create()
  }
}