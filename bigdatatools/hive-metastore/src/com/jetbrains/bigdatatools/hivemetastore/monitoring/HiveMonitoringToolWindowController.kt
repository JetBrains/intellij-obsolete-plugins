package com.jetbrains.bigdatatools.hivemetastore.monitoring

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowController
import com.jetbrains.bigdatatools.hivemetastore.monitoring.controllers.HiveDatabasesController
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveMetastoreConnectionData
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveMetastoreConnectionGroup
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveToolWindowSettings

@Service(Service.Level.PROJECT)
class HiveMonitoringToolWindowController(project: Project) : MonitoringToolWindowController(project) {
  override val settings
    get() = HiveToolWindowSettings.getInstance()

  override val helpTopicId: String = "big.data.tools.hive"

  override val toolWindowId: String = TOOL_WINDOW_ID

  override fun createConnectionGroup(): ConnectionFactory<*> = HiveMetastoreConnectionGroup()

  override fun isSupportedData(connectionData: ConnectionData): Boolean = connectionData is HiveMetastoreConnectionData

  override fun createMainController(connectionData: ConnectionData) =
    HiveDatabasesController(project,
                            connectionData as HiveMetastoreConnectionData).also {
      Disposer.register(this, it)
    }

  companion object {
    fun getInstance(project: Project): HiveMonitoringToolWindowController? = project.getService(
      HiveMonitoringToolWindowController::class.java)

    const val TOOL_WINDOW_ID = "HiveToolWindow"
  }
}