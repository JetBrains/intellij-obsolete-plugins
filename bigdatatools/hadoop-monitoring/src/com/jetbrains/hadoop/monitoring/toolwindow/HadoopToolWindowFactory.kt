package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowFactory
import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle

class HadoopToolWindowFactory : MonitoringToolWindowFactory() {
  override val toolWindowId: String = HadoopToolWindowController.TOOL_WINDOW_ID
  override val connectionType = BdtConnectionType.YARN
  override val title: String = HadoopMessagesBundle.message("toolwindow.title")

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    HadoopToolWindowController.getInstance(project).setUp(toolWindow)
  }
}