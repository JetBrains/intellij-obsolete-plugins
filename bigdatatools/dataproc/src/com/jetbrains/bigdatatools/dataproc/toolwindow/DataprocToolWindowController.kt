package com.jetbrains.bigdatatools.dataproc.toolwindow

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.jetbrains.bigdatatools.common.connection.updater.IntervalUpdateSettings
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowController
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.settings.DataprocConnectionData
import com.jetbrains.bigdatatools.dataproc.settings.DataprocConnectionGroup
import com.jetbrains.bigdatatools.dataproc.settings.DataprocToolWindowSettings
import com.jetbrains.bigdatatools.dataproc.toolwindow.controllers.DataprocClusterListController

@Service(Service.Level.PROJECT)
class DataprocToolWindowController(project: Project) : MonitoringToolWindowController(project) {
  override val settings: IntervalUpdateSettings
    get() = DataprocToolWindowSettings.getInstance()

  override val helpTopicId: String = "big.data.tools.gc.dataproc"

  override val toolWindowId: String = TOOL_WINDOW_ID

  override fun createConnectionGroup() = DataprocConnectionGroup()
  override fun isSupportedData(connectionData: ConnectionData): Boolean = connectionData is DataprocConnectionData

  override fun createMainController(connectionData: ConnectionData): DataprocClusterListController {
    val dataManager = DataprocDataManager.getInstance(connectionData.innerId, project) ?: error("Data Manager is not initialized")
    return DataprocClusterListController(project, dataManager)
  }

  override fun focusOn(connectionId: String) = focusOn(connectionId, null)

  fun focusOn(connectionId: String, clusterId: String?) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return

    toolWindow.show {
      val contentManager = toolWindow.contentManager
      val content = contentManager.contents.firstOrNull { it.getUserData(CONNECTION_ID) == connectionId } ?: return@show
      setSelectedContent(content)

      val mainController = content.getUserData(PAGE_CONTROLLER_ID) as? DataprocClusterListController ?: return@show
      clusterId ?: return@show

      val dataTable = mainController.dataTable
      val index = dataTable.tableModel.getDataModel()?.entries?.withIndex()?.firstOrNull { it.value.id == clusterId }?.index
                  ?: return@show
      dataTable.selectionModel.setSelectionInterval(index, index)
    }
  }

  companion object {
    fun getInstance(project: Project): DataprocToolWindowController? = project.getService(DataprocToolWindowController::class.java)

    const val TOOL_WINDOW_ID = "DataprocToolWindow"
  }
}