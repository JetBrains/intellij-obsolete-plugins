package com.jetbrains.bigdatatools.flink.graph

import com.intellij.diagram.DiagramBuilder
import com.intellij.diagram.DiagramColors
import com.intellij.diagram.DiagramNode
import com.intellij.diagram.extras.DiagramExtras
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.graph.GraphManager
import com.intellij.openapi.graph.layout.LayoutOrientation
import com.intellij.openapi.graph.layout.Layouter
import com.intellij.openapi.graph.services.GraphLayoutService
import com.intellij.openapi.graph.settings.GraphSettings
import com.intellij.openapi.graph.view.NodeRealizer
import com.intellij.openapi.project.Project
import com.intellij.ui.CellRendererPanel
import com.intellij.uml.core.actions.DiagramToolbarActionsProviderImpl
import com.intellij.util.ui.JBUI
import com.jetbrains.bigdatatools.flink.model.Node
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class FlinkDiagramExtras : DiagramExtras<Node>() {
  override fun getToolbarActionsProvider() = object : DiagramToolbarActionsProviderImpl() {
    override fun addToolbarActionsTo(group: DefaultActionGroup, builder: DiagramBuilder) =
      group.add(ActionManager.getInstance().getAction("Graph.JobGraphToolbar"))
  }

  override fun createNodeComponent(
    node: DiagramNode<Node>,
    builder: DiagramBuilder,
    nodeRealizer: NodeRealizer,
    wrapper: JPanel
  ): JComponent {
    val cellPanel = CellRendererPanel(BorderLayout())

    val colorScheme = builder.colorScheme
    cellPanel.border = BorderFactory.createLineBorder(colorScheme.getColor(DiagramColors.NODE_BORDER), 1)
    cellPanel.setForcedBackground(colorScheme.getColor(DiagramColors.NODE_HEADER))

    val descriptionText = JLabel(node.identifyingElement.nodeLabel)
    descriptionText.font = builder.provider.colorManager.getNodeHeaderFont(builder)
    descriptionText.border = JBUI.Borders.empty(3, 5)

    cellPanel.add(descriptionText, BorderLayout.CENTER)
    return cellPanel
  }

  override fun getCustomLayouter(settings: GraphSettings, project: Project): Layouter {
    val layouter = GraphManager.getGraphManager().createTreeLayouter()
    layouter.minimalNodeDistance = 80.0
    layouter.minimalLayerDistance = 100.0
    layouter.layoutOrientation = LayoutOrientation.RIGHT_TO_LEFT
    GraphLayoutService.getInstance().setEdgeLabeling(layouter, true)
    return layouter
  }
}