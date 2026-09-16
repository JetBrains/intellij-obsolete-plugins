package com.jetbrains.spark.monitoring.graph

import com.intellij.diagram.AbstractDiagramElementManager
import com.intellij.diagram.BaseDiagramProvider
import com.intellij.diagram.DiagramPresentationModel
import com.intellij.diagram.DiagramVfsResolver
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.graph.view.Graph2D
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.spark.monitoring.graph.model.DotDataModel
import com.jetbrains.spark.monitoring.graph.model.DotItem
import com.jetbrains.spark.monitoring.graph.view.DotPresentationModel
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import org.intellij.lang.annotations.Pattern

object DotDiagramProvider : BaseDiagramProvider<DotItem>() {
  @Pattern("[a-zA-Z0-9_-]*")
  override fun getID(): String = ID

  override fun getPresentableName(): String = SMMessagesBundle.message("graph.presentable.name")

  override fun createDataModel(project: Project,
                               element: DotItem?,
                               file: VirtualFile?,
                               presentationModel: DiagramPresentationModel) = DotDataModel(project, this)

  override fun createPresentationModel(project: Project, graph: Graph2D) = DotPresentationModel(project, graph)

  override fun getExtras() = DotDiagramExtras()

  override fun getElementManager() = object : AbstractDiagramElementManager<DotItem>() {
    override fun findInDataContext(context: DataContext): DotItem? = null

    override fun isAcceptableAsNode(element: Any?): Boolean = element is DotItem

    override fun getElementTitle(element: DotItem?): String? = element?.name

    override fun getNodeTooltip(element: DotItem?): String? = null
  }

  override fun getVfsResolver() = object : DiagramVfsResolver<DotItem> {
    override fun getQualifiedName(element: DotItem?): String? = null

    override fun resolveElementByFQN(fqn: String, project: Project): DotItem? = null
  }

  private const val ID = "SparkDotFiles"
}