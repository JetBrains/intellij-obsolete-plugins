package com.jetbrains.spark.monitoring.graph.model

import com.intellij.diagram.DiagramDataModel
import com.intellij.diagram.DiagramNode
import com.intellij.diagram.DiagramProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.jetbrains.spark.monitoring.graph.util.DotFormatParser

class DotDataModel(project: Project, diagramProvider: DiagramProvider<DotItem>) : DiagramDataModel<DotItem>(project, diagramProvider) {
  private var parser = DotFormatParser()

  override fun dispose() {}

  override fun getNodes() = parser.nodesWithId.values

  override fun getEdges() = parser.edges

  override fun getModificationTracker(): ModificationTracker = ModificationTracker.NEVER_CHANGED

  override fun addElement(element: DotItem?): DiagramNode<DotItem>? = null

  override fun getNodeName(node: DiagramNode<DotItem>) = node.identifyingElement.name

  override fun getGroup(node: DiagramNode<DotItem>) = node.identifyingElement.group
}