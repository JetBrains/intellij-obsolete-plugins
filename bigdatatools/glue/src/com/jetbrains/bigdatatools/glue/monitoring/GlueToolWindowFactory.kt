package com.jetbrains.bigdatatools.glue.monitoring

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowFactory
import com.jetbrains.bigdatatools.glue.utils.GlueMessagesBundle

class GlueToolWindowFactory : MonitoringToolWindowFactory() {
  override val toolWindowId = GlueMonitoringToolWindowController.TOOL_WINDOW_ID
  override val connectionType = BdtConnectionType.GLUE
  override val title: String = GlueMessagesBundle.message("toolwindow.title")

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    GlueMonitoringToolWindowController.getInstance(project)?.setUp(toolWindow)
  }
}