package com.jetbrains.spark.monitoring.graph.model

import com.intellij.diagram.DiagramEdgeBase
import com.intellij.diagram.DiagramRelationshipInfo
import com.intellij.diagram.DiagramRelationshipInfoAdapter
import com.intellij.diagram.presentation.DiagramLineType

class DotEdge(source: DotNode, target: DotNode) : DiagramEdgeBase<DotItem>(source, target, DOT_RELATIONSHIP) {
  companion object {
    val DOT_RELATIONSHIP: DiagramRelationshipInfoAdapter = DiagramRelationshipInfoAdapter.Builder()
      .setLineType(DiagramLineType.SOLID)
      .setTargetArrow(DiagramRelationshipInfo.STANDARD)
      .create()
  }
}