package com.jetbrains.bigdatatools.flink.toolwindow

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowController
import com.jetbrains.bigdatatools.flink.rfs.FlinkConnectionData
import com.jetbrains.bigdatatools.flink.settings.FlinkConnectionGroup
import com.jetbrains.bigdatatools.flink.toolwindow.config.FlinkToolWindowSettings
import com.jetbrains.bigdatatools.flink.toolwindow.controllers.FlinkMainPageController

@Service(Service.Level.PROJECT)
class FlinkMonitoringToolWindowController(project: Project) : MonitoringToolWindowController(project) {
  override val settings
    get() = FlinkToolWindowSettings.getInstance()

  override val helpTopicId: String = "big.data.tools.flink"

  override val toolWindowId: String = TOOL_WINDOW_ID

  override fun createConnectionGroup(): ConnectionFactory<*> = FlinkConnectionGroup()

  override fun isSupportedData(connectionData: ConnectionData): Boolean = connectionData is FlinkConnectionData

  override fun createMainController(connectionData: ConnectionData) = FlinkMainPageController(project,
                                                                                                  connectionData as FlinkConnectionData).also {
    Disposer.register(this, it)
  }

  companion object {
    fun getInstance(project: Project): FlinkMonitoringToolWindowController? = project.getService(
      FlinkMonitoringToolWindowController::class.java)

    const val TOOL_WINDOW_ID = "FlinkToolWindow"
  }
}