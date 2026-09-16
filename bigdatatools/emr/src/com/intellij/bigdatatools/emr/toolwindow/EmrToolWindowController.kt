package com.intellij.bigdatatools.emr.toolwindow

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.settings.EmrConnectionData
import com.intellij.bigdatatools.emr.settings.EmrConnectionGroup
import com.intellij.bigdatatools.emr.settings.EmrToolWindowSettings
import com.intellij.bigdatatools.emr.toolwindow.controllers.EmrClusterController
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.jetbrains.bigdatatools.common.connection.updater.IntervalUpdateSettings
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowController

@Service(Service.Level.PROJECT)
class EmrToolWindowController(project: Project) : MonitoringToolWindowController(project) {
  override val settings: IntervalUpdateSettings
    get() = EmrToolWindowSettings.getInstance()

  override val helpTopicId: String = "big.data.tools.amazon.emr"

  override val toolWindowId: String = TOOL_WINDOW_ID

  override fun createConnectionGroup(): ConnectionFactory<*> = EmrConnectionGroup()
  override fun isSupportedData(connectionData: ConnectionData): Boolean = connectionData is EmrConnectionData

  override fun createMainController(connectionData: ConnectionData): EmrClusterController {
    val dataManager = EmrDataManager.getInstance(connectionData.innerId, project) ?: error("Data Manager is not initialized")
    return EmrClusterController(project, dataManager)
  }

  override fun focusOn(connectionId: String) = focusOn(connectionId, null)

  fun focusOn(connectionId: String, clusterId: String?) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return

    toolWindow.show {
      val contentManager = toolWindow.contentManager
      val content = contentManager.contents.firstOrNull { it.getUserData(CONNECTION_ID) == connectionId } ?: return@show
      setSelectedContent(content)

      val mainController = content.getUserData(PAGE_CONTROLLER_ID) as? EmrClusterController ?: return@show
      clusterId ?: return@show

      val dataTable = mainController.dataTable
      val index = dataTable.tableModel.getDataModel()?.entries?.withIndex()?.firstOrNull { it.value.id == clusterId }?.index
                  ?: return@show
      dataTable.selectionModel.setSelectionInterval(index, index)
    }
  }

  companion object {
    fun getInstance(project: Project): EmrToolWindowController? = project.getService(EmrToolWindowController::class.java)

    const val TOOL_WINDOW_ID = "EmrToolWindow"
  }
}