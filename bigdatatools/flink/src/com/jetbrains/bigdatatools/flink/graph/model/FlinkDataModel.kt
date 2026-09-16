package com.jetbrains.bigdatatools.flink.graph.model

import com.intellij.diagram.DiagramDataModel
import com.intellij.diagram.DiagramNode
import com.intellij.diagram.DiagramProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.jetbrains.bigdatatools.flink.graph.util.JobDetailsInfoParser
import com.jetbrains.bigdatatools.flink.model.Node

class FlinkDataModel(project: Project, diagramProvider: DiagramProvider<Node>) : DiagramDataModel<Node>(project, diagramProvider) {
  private var parser = JobDetailsInfoParser()

  override fun dispose() {}

  override fun getNodes() = parser.nodesWithId.values

  override fun getEdges() = parser.edges

  override fun getModificationTracker(): ModificationTracker = ModificationTracker.NEVER_CHANGED

  override fun addElement(element: Node?) = null

  override fun getNodeName(node: DiagramNode<Node>) = node.identifyingElement.description
}