package com.jetbrains.bigdatatools.glue.monitoring

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowController
import com.jetbrains.bigdatatools.glue.monitoring.controllers.GlueDatabasesController
import com.jetbrains.bigdatatools.glue.settings.GlueConnectionData
import com.jetbrains.bigdatatools.glue.settings.GlueConnectionGroup
import com.jetbrains.bigdatatools.glue.settings.GlueToolWindowSettings

@Service(Service.Level.PROJECT)
class GlueMonitoringToolWindowController(project: Project) : MonitoringToolWindowController(project) {
  override val settings
    get() = GlueToolWindowSettings.getInstance()

  override val helpTopicId: String = "big.data.tools.glue"

  override val toolWindowId: String = TOOL_WINDOW_ID

  override fun createConnectionGroup(): ConnectionFactory<*> = GlueConnectionGroup()

  override fun isSupportedData(connectionData: ConnectionData): Boolean = connectionData is GlueConnectionData

  override fun createMainController(connectionData: ConnectionData) =
    GlueDatabasesController(project, connectionData as GlueConnectionData).also {
      Disposer.register(this, it)
    }

  companion object {
    fun getInstance(project: Project): GlueMonitoringToolWindowController? = project.getService(
      GlueMonitoringToolWindowController::class.java)

    const val TOOL_WINDOW_ID = "GlueToolWindow"
  }
}