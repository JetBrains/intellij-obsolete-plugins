package com.jetbrains.bigdatatools.flink.graph.model

import com.intellij.diagram.DiagramNodeBase
import com.intellij.diagram.DiagramProvider
import com.jetbrains.bigdatatools.flink.model.Node
import javax.swing.Icon

class FlinkNode(private val element: Node, provider: DiagramProvider<Node>) : DiagramNodeBase<Node>(provider) {
  override fun getIcon(): Icon? = null

  override fun getIdentifyingElement(): Node = element

  override fun getTooltip(): String? = null
}