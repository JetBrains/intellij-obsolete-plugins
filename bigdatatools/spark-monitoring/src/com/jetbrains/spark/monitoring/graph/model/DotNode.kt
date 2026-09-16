package com.jetbrains.spark.monitoring.graph.model

import com.intellij.diagram.DiagramNodeBase
import com.intellij.diagram.DiagramProvider
import javax.swing.Icon

class DotNode(private val element: DotItem, provider: DiagramProvider<DotItem>) : DiagramNodeBase<DotItem>(provider) {
  override fun getIcon(): Icon? = null

  override fun getIdentifyingElement(): DotItem = element

  override fun getTooltip(): String? = null
}