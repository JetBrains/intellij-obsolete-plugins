package com.jetbrains.spark.monitoring.graph

import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.diagram.DiagramBuilder
import com.intellij.diagram.DiagramColors
import com.intellij.diagram.DiagramNode
import com.intellij.diagram.extras.DiagramExtras
import com.intellij.diagram.extras.EditNodeHandler
import com.intellij.execution.filters.HyperlinkInfoFactory
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.readAction
import com.intellij.openapi.graph.GraphManager
import com.intellij.openapi.graph.layout.Layouter
import com.intellij.openapi.graph.settings.GraphSettings
import com.intellij.openapi.graph.view.NodeRealizer
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.CellRendererPanel
import com.intellij.uml.core.actions.DiagramToolbarActionsProviderImpl
import com.intellij.util.ui.JBUI
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.spark.monitoring.graph.model.DotItem
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import org.jsoup.Jsoup
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class DotDiagramExtras : DiagramExtras<DotItem>() {
  override fun getToolbarActionsProvider() = object : DiagramToolbarActionsProviderImpl() {
    override fun addToolbarActionsTo(group: DefaultActionGroup, builder: DiagramBuilder) =
      group.add(ActionManager.getInstance().getAction("Graph.JobGraphToolbar"))
  }

  override fun getCustomLayouter(settings: GraphSettings, project: Project): Layouter {
    val layouter = GraphManager.getGraphManager().createHierarchicGroupLayouter()
    layouter.minimalLayerDistance = 30.0
    layouter.layerer = GraphManager.getGraphManager().createBFSLayerer()

    val layouterWrapper = GraphManager.getGraphManager().createGraphLayoutLineWrapper()
    layouterWrapper.isColumnMode = true
    layouterWrapper.isMirroringEnabled = false
    layouterWrapper.spacing = 50.0
    layouterWrapper.coreLayouter = layouter

    return layouterWrapper
  }

  override fun createNodeComponent(
    node: DiagramNode<DotItem>,
    builder: DiagramBuilder,
    nodeRealizer: NodeRealizer,
    wrapper: JPanel
  ): JComponent {
    val cellPanel = CellRendererPanel(BorderLayout())

    val colorScheme = builder.colorScheme
    cellPanel.border = BorderFactory.createLineBorder(colorScheme.getColor(DiagramColors.NODE_BORDER), 1)
    cellPanel.setForcedBackground(colorScheme.getColor(DiagramColors.NODE_HEADER))

    val presentationText = JLabel(node.identifyingElement.name)
    presentationText.font = builder.provider.colorManager.getNodeHeaderFont(builder)
    presentationText.border = JBUI.Borders.empty(3, 5)
    cellPanel.add(presentationText, BorderLayout.CENTER)
    return cellPanel
  }

  override fun getEditNodeHandler(): EditNodeHandler<DotItem> = EditNodeHandler<DotItem> { node, presentationModel ->
    val project = presentationModel.diagramBuilder.project

    //parsedLines - ParallelCollectionRDD [0] parallelize at SparkPi.scala:34
    val parsedLines = Jsoup.parse(node.identifyingElement.name).text().split(" ")
    val (fileName, logicalLine) = parsedLines[4].split(":")

    executeOnPooledThread {
      val files = runBlockingMaybeCancellable {
        readAction {
          FilenameIndex.getVirtualFilesByName(fileName, true, GlobalSearchScope.allScope(project)).toList()
        }
      }

      invokeLater {
        if (files.isEmpty()) {
          Messages.showInfoMessage(project, SMMessagesBundle.message("graph.cannot.navigate.text", fileName, project.name),
                                   SMMessagesBundle.message("graph.cannot.navigate.title"))
        }
        else {
          val line = logicalLine.toIntOrNull()?.minus(1) ?: 0
          val info = HyperlinkInfoFactory.getInstance().createMultipleFilesHyperlinkInfo(files, line, project, null)
          info.navigate(project)
        }
      }
    }
  }
}