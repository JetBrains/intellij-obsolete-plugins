package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.ui.content.Content
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowController
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppInfo
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionGroup
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings

@Service(Service.Level.PROJECT)
class HadoopToolWindowController(project: Project) : MonitoringToolWindowController(project) {
  override val settings
    get() = HadoopSettings.getInstance()

  override val helpTopicId: String = "big.data.tools.hadoop.monitoring"

  override val toolWindowId: String = TOOL_WINDOW_ID

  override fun createConnectionGroup(): ConnectionFactory<*> = HadoopConnectionGroup()

  override fun isSupportedData(connectionData: ConnectionData): Boolean = connectionData is HadoopConnectionData

  override fun createMainController(connectionData: ConnectionData) = HadoopPageController(project, connectionData as HadoopConnectionData)

  /** Shows new tab with selected application info or switches to existing tab. */
  fun showApplication(connectionData: HadoopConnectionData, app: AppInfo) {
    val found = contentManager.contents.find { content -> content.getUserData(APPLICATION_ID) == app.id }

    if (found == null) {
      val panel = createApplicationDetailsPanel(project, connectionData, app)
      contentManager.addContent(panel)
      contentManager.setSelectedContent(panel)
    }
    else {
      setSelectedContent(found)
    }
  }

  private fun createApplicationDetailsPanel(project: Project, connectionData: HadoopConnectionData, appInfo: AppInfo): Content {
    val applicationDetails = ApplicationPage(project, connectionData, appInfo)

    @Suppress("HardCodedStringLiteral") // Display name is the name of user app.
    val content = contentManager.factory.createContent(applicationDetails.getComponent(), appInfo.name, false).apply {
      putUserData(APPLICATION_ID, appInfo.id)
      putUserData(CONNECTION_ID, connectionData.innerId)
    }

    Disposer.register(content, applicationDetails)

    return content
  }

  companion object {
    const val TOOL_WINDOW_ID = "hadoop-toolwindow"
    val APPLICATION_ID = Key.create<String>("APPLICATION_ID")

    fun getInstance(project: Project): HadoopToolWindowController = project.getService(HadoopToolWindowController::class.java)
  }
}