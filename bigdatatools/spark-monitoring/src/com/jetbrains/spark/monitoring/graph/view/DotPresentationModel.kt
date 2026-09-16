package com.jetbrains.spark.monitoring.graph.view

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.graph.view.Graph2D
import com.intellij.openapi.project.Project
import com.intellij.uml.presentation.DiagramPresentationModelImpl
import com.jetbrains.spark.monitoring.graph.DotDiagramProvider

class DotPresentationModel(project: Project, graph: Graph2D) : DiagramPresentationModelImpl(graph, project, DotDiagramProvider) {
  init {
    settings.isFitContentAfterLayout = true
    settings.isShowEdgeLabels = false
  }

  override fun getCommonActionGroup(): DefaultActionGroup =
    ActionManager.getInstance().getAction("Graph.JobGraphPopup") as DefaultActionGroup
}