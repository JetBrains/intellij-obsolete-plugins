package com.jetbrains.bigdatatools.flink.graph.view

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.graph.view.Graph2D
import com.intellij.openapi.project.Project
import com.intellij.uml.presentation.DiagramPresentationModelImpl
import com.jetbrains.bigdatatools.flink.graph.FlinkDiagramProvider

internal class FlinkPresentationModel(
  project: Project,
  graph: Graph2D,
) : DiagramPresentationModelImpl(graph, project, FlinkDiagramProvider) {
  init {
    settings.isFitContentAfterLayout = true
    settings.isShowEdgeLabels = true
  }

  override fun getCommonActionGroup(): DefaultActionGroup {
    return ActionManager.getInstance().getAction("Graph.JobGraphPopup") as DefaultActionGroup
  }
}