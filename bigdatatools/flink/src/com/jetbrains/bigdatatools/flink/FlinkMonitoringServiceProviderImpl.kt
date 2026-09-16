package com.jetbrains.bigdatatools.flink

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.integration.MonitoringOpenOptions
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.bigdatatools.flink.rfs.FlinkConnectionData
import com.jetbrains.bigdatatools.flink.settings.FlinkConnectionGroup
import com.jetbrains.bigdatatools.flink.toolwindow.FlinkMonitoringToolWindowController
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle
import java.net.URL

class FlinkMonitoringServiceProviderImpl : MonitoringServiceProvider {
  override fun isSupport(groupId: String): Boolean {
    return groupId == BdtConnectionType.FLINK.id
  }

  override fun openNewConnectionSettings(groupId: String, project: Project, perProject: Boolean): ConnectionData? {
    if (!isSupport(groupId))
      return null

    val connectionGroup = FlinkConnectionGroup()
    return ConnectionSettings.create(project, connectionGroup, connectionGroup.createBlankData(perProject = perProject),
                                     applyIfOk = true)
  }

  override fun typeOfMonitoring(): String {
    return "Flink"
  }

  override fun collectStatistics(project: Project) {}

  override fun createConnection(project: Project, trackingUrl: String, selectOption: MonitoringOpenOptions): ConnectionData? {
    val connectionGroup = FlinkConnectionGroup()
    val connectionData = connectionGroup.createBlankData(perProject = selectOption.isPerProject).apply {
      val jobUrl = URL(trackingUrl)
      uri = jobUrl.host + (if (jobUrl.port != -1) ":" + jobUrl.port else "")
      selectOption.tunnelData?.let { setTunnelData(it) }
      name = FlinkMessagesBundle.message("connection.default.name", jobUrl.host)
    }
    return ConnectionSettings.create(project, connectionGroup, connectionData, applyIfOk = true)
  }

  override fun openToolWindowController(project: Project, trackingUrl: String, connection: ConnectionData) {
    val toolWindowController = FlinkMonitoringToolWindowController.getInstance(project)
    toolWindowController?.focusOn(connection.innerId)
  }

  override fun getConnectionById(project: Project, monitoringDriverId: String): ConnectionData? {
    return RfsConnectionDataManager.instance?.getTyped<FlinkConnectionData>(monitoringDriverId, project)
  }

  override fun getConnectionsByProject(project: Project): List<ConnectionData>? {
    return RfsConnectionDataManager.instance?.getTyped<FlinkConnectionData>(project)
  }
}