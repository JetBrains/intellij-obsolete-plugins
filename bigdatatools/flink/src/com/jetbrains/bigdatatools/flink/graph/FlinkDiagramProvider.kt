package com.jetbrains.bigdatatools.flink.graph

import com.intellij.diagram.AbstractDiagramElementManager
import com.intellij.diagram.BaseDiagramProvider
import com.intellij.diagram.DiagramPresentationModel
import com.intellij.diagram.DiagramVfsResolver
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.graph.view.Graph2D
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.bigdatatools.flink.graph.model.FlinkDataModel
import com.jetbrains.bigdatatools.flink.graph.view.FlinkPresentationModel
import com.jetbrains.bigdatatools.flink.model.Node
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle
import org.intellij.lang.annotations.Pattern

internal object FlinkDiagramProvider : BaseDiagramProvider<Node>() {
  @Pattern("[a-zA-Z0-9_-]*")
  override fun getID(): String = ID

  override fun getPresentableName(): String = FlinkMessagesBundle.message("graph.presentable.name")

  override fun createDataModel(project: Project,
                               element: Node?,
                               file: VirtualFile?,
                               presentationModel: DiagramPresentationModel) = FlinkDataModel(project, this)

  override fun createPresentationModel(project: Project, graph: Graph2D) = FlinkPresentationModel(project, graph)

  override fun getExtras() = FlinkDiagramExtras()

  override fun getElementManager() = object : AbstractDiagramElementManager<Node>() {
    override fun findInDataContext(context: DataContext): Node? = null

    override fun isAcceptableAsNode(element: Any?): Boolean = element is Node

    override fun getElementTitle(element: Node?): String? = element?.description

    override fun getNodeTooltip(element: Node?): String? = null
  }

  override fun getVfsResolver() = object : DiagramVfsResolver<Node> {
    override fun getQualifiedName(element: Node?): String? = null

    override fun resolveElementByFQN(fqn: String, project: Project): Node? = null
  }

  private const val ID = "FlinkGraphFiles"
}