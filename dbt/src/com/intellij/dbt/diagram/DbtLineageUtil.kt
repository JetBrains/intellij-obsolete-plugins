package com.intellij.dbt.diagram

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.dbt.DbtUtils
import com.intellij.dbt.DbtUtils.Companion.getAllModels
import com.intellij.dbt.DbtUtils.Companion.getAllSeeds
import com.intellij.dbt.console.DbtConsoleService
import com.intellij.diagram.v2.GraphChartFactory
import com.intellij.diagram.v2.handles.GraphChartHandle
import com.intellij.diagram.v2.handles.GraphChartUpdateHandle.GraphChartLayoutQueryParams.FitContentOption
import com.intellij.diagram.v2.layout.GraphChartLayoutOrientation
import com.intellij.diagram.v2.layout.GraphChartLayoutService
import com.intellij.diagram.v2.painting.GraphChartPainterService
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.pom.Navigatable
import com.intellij.util.graph.GraphFactory
import com.intellij.util.graph.Network
import icons.DatabaseIcons
import org.jetbrains.annotations.NonNls
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.CompletableFuture.completedFuture
import javax.swing.Icon


open class DbtLineageNode(@NlsSafe @NonNls val pkg: String? = null, @NlsSafe @NonNls val name: String, protected val module: Module): Navigatable {
  open val icon: Icon? = null
  open val type: String? = null

  override fun canNavigate(): Boolean {
    return true
  }
}

class DbtLineageSeedNode(@NonNls pkg: String? = null, @NonNls name: String, module: Module) : DbtLineageNode(pkg, name, module) {
  override val icon = AllIcons.FileTypes.Csv
  override val type = "seed"

  override fun navigate(requestFocus: Boolean) {
    val targetFile = getAllSeeds(module).filter { it.nameWithoutExtension == name}.firstOrNull() ?: return
    FileEditorManager.getInstance(module.project).openFile(targetFile)
  }
}

class DbtLineageModelNode(@NonNls pkg: String? = null, @NonNls name: String, module: Module) : DbtLineageNode(pkg, name, module) {
  override val icon = DatabaseIcons.Table
  override val type = "model"

  override fun navigate(requestFocus: Boolean) {
    val targetFile = getAllModels(module).filter { it.nameWithoutExtension == name}.firstOrNull() ?: return
    FileEditorManager.getInstance(module.project).openFile(targetFile)
  }
}

fun trimNodeName(nodeName: String): String {
  if (nodeName.startsWith("model.") || nodeName.startsWith("seed.") || nodeName.startsWith("test.")) {
    return nodeName.substring(nodeName.indexOf('.') + 1)
  }
  return nodeName
}

fun splitPackageAndName(fqn: String): Pair<String?, String> {
  val idx = fqn.lastIndexOf('.')
  if (idx > 0 && idx < fqn.length - 1) {
    return Pair(fqn.substring(0, idx), fqn.substring(idx + 1, fqn.length))
  }
  return Pair(null, fqn)
}

private const val NODE_TYPE_MODEL = "model"
private const val NODE_TYPE_SEED = "seed"

fun readLineage(graphSummaryContent: String, module: Module) : Network<DbtLineageNode, Any>? {
  val mapper = jacksonObjectMapper()
  val dbtGraphSummaryContainerObject: DbtGraphSummaryContainerObject
  try {
    dbtGraphSummaryContainerObject = mapper.readValue(graphSummaryContent)
  } catch (e: Exception) {
    return null
  }
  val nodes = dbtGraphSummaryContainerObject.linked
  val graph = GraphFactory.getInstance().directedNetwork().build<DbtLineageNode, Any>()

  var edgeCount = 0
  val diagramNodes = mutableMapOf<String, DbtLineageNode>()
  nodes.forEach {
    val nodeFQN = trimNodeName(it.value.name)
    val pkgAndName = splitPackageAndName(nodeFQN)

    val diagramNode = when (it.value.type) {
      NODE_TYPE_MODEL -> DbtLineageModelNode(pkgAndName.first, pkgAndName.second, module)
      NODE_TYPE_SEED -> DbtLineageSeedNode(pkgAndName.first, pkgAndName.second, module)
      else -> DbtLineageNode(pkgAndName.first, pkgAndName.second, module)
    }
    diagramNodes[it.key] = diagramNode
  }

  val typeFilter = listOf(NODE_TYPE_MODEL, NODE_TYPE_SEED)
  nodes.forEach {
    if (it.value.type in typeFilter) {
      val successors = it.value.succ
      if (successors != null) {
        for (nr in successors) {
          val linked = nodes["$nr"]
          if (linked != null && linked.type in typeFilter) {
            edgeCount++
            graph.addEdge(diagramNodes[it.key], diagramNodes["$nr"], edgeCount)
          }
        }
      }
    }
  }

  return graph
}

data class DbtGraphSummaryNode(
  val name: String,
  val type: String,
  val succ: List<Int>?
)

data class DbtGraphSummaryContainerObject(
  @JsonProperty("_invocation_id") val invocationId: String,
  val linked: Map<String, DbtGraphSummaryNode>
)

fun openLineage(virtualFile: VirtualFile?, module: Module, forced: Boolean = false) {
  val dbtSettings = DbtUtils.getDbtSettings(module) ?: return
  val dbtService = service<DbtConsoleService>()

  val project = module.project
  if (!forced && dbtService.isDbtToolwindowCreated(project) && !dbtService.isLineageVisible(project)) {
    return
  }
  val projectPath = dbtSettings.dbtProjectPath?.virtualFile?.path ?: return
  val path = "$projectPath/target/graph_summary.json".toNioPathOrNull()
  if (path == null) {
    dbtService.setGraphHash(module, 0)
    return
  }
  if (!path.toFile().exists()) return

  val graphSummaryContent: String
  try {
    graphSummaryContent = Files.readString(path)
  }
  catch (e: IOException) {
    return
  }
  val newGraphSummaryHash = graphSummaryContent.hashCode()

  if (!forced && dbtService.getGraphHash(module) == newGraphSummaryHash) {
    if (newGraphSummaryHash != 0 && virtualFile != null && DbtUtils.isUnderModelsDirectory(virtualFile, module)) {
      val graphHandle = dbtService.getGraphHandle(module)
      if (graphHandle != null) {
        ApplicationManager.getApplication().invokeLater {
          selectNodeForFile(graphHandle, virtualFile)
        }
      }
    }
    return
  }

  if (virtualFile != null && !DbtUtils.isUnderModelsDirectory(virtualFile, module)) {
    return
  }

  val lineageGraph = readLineage(graphSummaryContent, module)
  if (lineageGraph == null) {
    dbtService.setGraphHash(module, 0)
    return
  }

  val chartConfiguration = GraphChartFactory.getInstance().graphChart(project, lineageGraph) {
    initialViewSettings {
      currentLayouter = GraphChartLayoutService.getInstance().hierarchicLayouter
      currentLayoutOrientation = GraphChartLayoutOrientation.LEFT_TO_RIGHT
    }

    nodePainter {
      labelWithIconNodePainter { _, node ->
        GraphChartPainterService.LabelWithIconNodeStyleProvider.LabelWithIcon(node.icon, node.name, null)
      }
    }

    tooltipProvider {
      htmlBasedNodePopup(fun(_: GraphChartHandle<DbtLineageNode, Any>, node: DbtLineageNode): HtmlChunk {
        val builder = HtmlBuilder()
        if (node.pkg != null) {
          builder.appendRaw(node.pkg).appendRaw(".")
        }
        return builder.append(node.name).toFragment()
      })
    }
  }

  GraphChartFactory.getInstance().instantiateAndGetComponent(chartConfiguration).thenApply {
    val graphHandle = it.first
    graphHandle.asSelectionHandle().addSelectionListener(dbtService) { event ->
      if (!dbtService.isLineageVisible(project)) {
        return@addSelectionListener
      }
      val element = event.elementWhichSelectionChanged
      if (element is com.intellij.diagram.v2.elements.GraphChartGraphElement.Node) {
        val node = element.node
        val isSelected = graphHandle.asSelectionHandle().selectedNodes.contains(node)
        if (isSelected) {
          ApplicationManager.getApplication().invokeLater {
            node.navigate(true)
          }
        }
      }
    }

    ApplicationManager.getApplication().invokeLater {
      dbtService.showComponent(project, it.second)
      dbtService.setLineageInfo(module, graphHandle, newGraphSummaryHash)
      selectNodeForFile(graphHandle, virtualFile)
    }
    graphHandle
  }.thenCompose { handle: GraphChartHandle<DbtLineageNode, Any> ->
    handle.asUpdateHandle().queryLayout().withFitContent(FitContentOption.BEFORE).run()

    ApplicationManager.getApplication().invokeLater {
      handle.asViewActionsHandle().fitContent(false)
    }

    completedFuture(null)
  }
}

private fun selectNodeForFile(graphHandle: GraphChartHandle<DbtLineageNode, Any>,
                              virtualFile: VirtualFile?) {
  val graph = graphHandle.graph
  val selectedNode = graph.nodes().firstOrNull { it.name == virtualFile?.nameWithoutExtension }
  if (selectedNode != null) {
    val asSelectionHandle = graphHandle.asSelectionHandle()
    asSelectionHandle.selectedNodes.forEach {
      asSelectionHandle.switchNodeSelection(it, false)
    }
    asSelectionHandle.switchNodeSelection(selectedNode, true)
  }
}
