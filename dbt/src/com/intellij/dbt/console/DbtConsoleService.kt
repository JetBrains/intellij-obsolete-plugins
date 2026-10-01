package com.intellij.dbt.console

import com.intellij.dbt.DbtBundle
import com.intellij.dbt.console.commands.DbtCommand
import com.intellij.dbt.diagram.DbtLineageNode
import com.intellij.diagram.v2.handles.GraphChartHandle
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.impl.ViewImpl
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.search.GlobalSearchScope
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import javax.swing.JComponent

private const val RUNNER_ID = "DbtRunner"
private const val LINEAGE_ID = "DbtLineage"
private val LOG = Logger.getInstance(DbtProcessHandler::class.java)

@Service
class DbtConsoleService : Disposable {
  private val graphsMap = ConcurrentHashMap<Module, LineAgeInfo>()

  companion object {
    private const val DBT_CONSOLE_TOOLWINDOW_ID = "DBT Console"
  }

  fun isLineageVisible(project: Project): Boolean {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(DBT_CONSOLE_TOOLWINDOW_ID)
    if (toolWindow == null || !toolWindow.isVisible) {
      return false
    }
    val lineageContent = findLineageContent(toolWindow) ?: return false
    return toolWindow.contentManager.selectedContent == lineageContent
  }

  fun isDbtToolwindowCreated(project: Project): Boolean {
    return ToolWindowManager.getInstance(project).getToolWindow(DBT_CONSOLE_TOOLWINDOW_ID)  != null
  }

  fun showConsoleViewContent(project: Project, processHandler: ProcessHandler) {
    createContent(project, processHandler, DbtCommand.COMPILE)
    processHandler.startNotify()
  }

  fun showComponent(project: Project, component: JComponent) {
    val toolWindow = getToolWindow(project)
    val contentManager = toolWindow.contentManager

    val lineageContent = findLineageContent(toolWindow)
    if (lineageContent != null) {
      contentManager.removeContent(lineageContent, true)
    }

    val ui = RunnerLayoutUi.Factory.getInstance(project).create(RUNNER_ID, "", "", this)
    val content = ui.createContent(LINEAGE_ID, component, DbtBundle.message("dbt.lineage.title"), null, component)
    content.isCloseable = false

    ui.addContent(content)
    contentManager.addContent(content)
    contentManager.setSelectedContent(content)
    toolWindow.show()
    toolWindow.component.requestFocus()
  }

  private fun findLineageContent(toolWindow: ToolWindow) =
    toolWindow.contentManager.contents.firstOrNull { it.getUserData(ViewImpl.ID) == LINEAGE_ID }

  private fun createContent(project: Project, handler: ProcessHandler, command: DbtCommand) {
    val toolWindow = getToolWindow(project)
    val console = ConsoleViewImpl(project, GlobalSearchScope.allScope(project), true, true)
    val ui = RunnerLayoutUi.Factory.getInstance(project).create(RUNNER_ID, "", "", this)
    val consoleContent = ui.createContent(RUNNER_ID, console.component, command.commandName, null, console.preferredFocusableComponent)
    console.attachToProcess(handler)

    ui.addContent(consoleContent)
    val contentManager = toolWindow.contentManager
    contentManager.addContent(consoleContent)
    contentManager.setSelectedContent(consoleContent)
    toolWindow.show()
  }

  private fun getToolWindow(project: Project): ToolWindow {
    val toolWindowManager = ToolWindowManager.getInstance(project)
    return toolWindowManager.getToolWindow(DBT_CONSOLE_TOOLWINDOW_ID)
           ?: toolWindowManager.registerToolWindow(
             RegisterToolWindowTask(id = DBT_CONSOLE_TOOLWINDOW_ID,
                                    canCloseContent = true,
                                    anchor = ToolWindowAnchor.BOTTOM,
                                    icon = AllIcons.Toolwindows.ToolWindowDataView,
                                    stripeTitle = Supplier { DbtBundle.message("toolwindow.title.DbtConsole") }))

  }

  override fun dispose() {
    graphsMap.clear()
  }

  fun getGraphHash(module: Module): Int {
    return graphsMap[module]?.hash ?: 0
  }

  fun getGraphHandle(module: Module): GraphChartHandle<DbtLineageNode, Any>? {
    return graphsMap[module]?.graphHandle
  }

  fun setLineageInfo(module: Module, graphHandle: GraphChartHandle<DbtLineageNode, Any>, hash: Int) {
    graphsMap[module] = LineAgeInfo(graphHandle, hash)
  }

  fun setGraphHash(module: Module, value: Int) {
    val lineAgeInfo = graphsMap[module]
    if (lineAgeInfo != null) {
      lineAgeInfo.hash = value
    } else {
      graphsMap[module] = LineAgeInfo(null, value)
    }
  }
}

class DbtProcessHandler(commandLine: GeneralCommandLine) : KillableColoredProcessHandler(commandLine) {
  init {
    addProcessListener(object : ProcessAdapter() {
      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        LOG.debug { event.text.trimEnd().trimStart('\r', '\n') }
      }
    })
  }
}

class LineAgeInfo(var graphHandle: GraphChartHandle<DbtLineageNode, Any>?, var hash: Int)