package com.jetbrains.bigdatatools.dataproc.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowFactory
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle

class DataprocToolWindowFactory : MonitoringToolWindowFactory() {
  override val toolWindowId: String = DataprocToolWindowController.TOOL_WINDOW_ID
  override val connectionType = BdtConnectionType.DATAPROC
  override val title: String = DataprocMessagesBundle.message("dataproc.toolwindow.title")

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    DataprocToolWindowController.getInstance(project)?.setUp(toolWindow)
  }
}