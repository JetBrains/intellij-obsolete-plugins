package com.intellij.bigdatatools.databricks.toolwindow

import com.intellij.bigdatatools.databricks.rfs.DatabricksConnectionData
import com.intellij.bigdatatools.databricks.settings.DatabricksConnectionGroup
import com.intellij.bigdatatools.databricks.toolwindow.config.DatabricksToolWindowSettings
import com.intellij.bigdatatools.databricks.toolwindow.controllers.DatabricksMainController
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.ComponentController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowController
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath

@Service(Service.Level.PROJECT)
class DatabricksMonitoringToolWindowController(project: Project) : MonitoringToolWindowController(project) {
  override val settings: DatabricksToolWindowSettings
    get() = DatabricksToolWindowSettings.getInstance()

  override val helpTopicId: String = "big.data.tools.databricks"

  override val toolWindowId: String = TOOL_WINDOW_ID

  fun focusOn(connectionId: String, rfsPath: RfsPath?) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return

    toolWindow.show {
      val contentManager = toolWindow.contentManager
      val content = contentManager.contents.firstOrNull { it.getUserData(CONNECTION_ID) == connectionId } ?: return@show
      setSelectedContent(content)

      if (rfsPath != null) {
        val mainController = content.getUserData(PAGE_CONTROLLER_ID) as? DatabricksMainController ?: return@show
        mainController.open(rfsPath)
      }
    }
  }

  fun getActiveTabConnectionId() : String? {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return null
    return toolWindow.contentManager.selectedContent?.getUserData(CONNECTION_ID)
  }

  override fun createConnectionGroup(): ConnectionFactory<*> = DatabricksConnectionGroup()

  override fun isSupportedData(connectionData: ConnectionData) = connectionData is DatabricksConnectionData

  override fun createMainController(connectionData: ConnectionData): ComponentController =
    DatabricksMainController(project, connectionData as DatabricksConnectionData)

  companion object {
    fun getInstance(project: Project): DatabricksMonitoringToolWindowController? = project.getService(
      DatabricksMonitoringToolWindowController::class.java)

    const val TOOL_WINDOW_ID = "DatabricksToolWindow"
  }
}