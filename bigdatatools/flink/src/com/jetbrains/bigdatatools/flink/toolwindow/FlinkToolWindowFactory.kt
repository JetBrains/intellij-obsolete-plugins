package com.jetbrains.bigdatatools.flink.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowFactory
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle

class FlinkToolWindowFactory : MonitoringToolWindowFactory() {
  override val toolWindowId = FlinkMonitoringToolWindowController.TOOL_WINDOW_ID
  override val connectionType = BdtConnectionType.FLINK
  override val title: String = FlinkMessagesBundle.message("toolwindow.title")

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    FlinkMonitoringToolWindowController.getInstance(project)?.setUp(toolWindow)
  }
}