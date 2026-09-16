package com.jetbrains.bigdatatools.hivemetastore.monitoring

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowFactory
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMessagesBundle

class HiveToolWindowFactory : MonitoringToolWindowFactory() {
  override val toolWindowId = HiveMonitoringToolWindowController.TOOL_WINDOW_ID
  override val connectionType = BdtConnectionType.HIVE
  override val title: String = HiveMessagesBundle.message("toolwindow.title")

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    HiveMonitoringToolWindowController.getInstance(project)?.setUp(toolWindow)
  }
}