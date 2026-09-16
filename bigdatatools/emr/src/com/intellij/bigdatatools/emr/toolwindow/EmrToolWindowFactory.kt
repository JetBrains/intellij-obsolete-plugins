package com.intellij.bigdatatools.emr.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowFactory
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle

class EmrToolWindowFactory : MonitoringToolWindowFactory() {
  override val toolWindowId: String = EmrToolWindowController.TOOL_WINDOW_ID
  override val connectionType = BdtConnectionType.EMR
  override val title: String = HdfsMessagesBundle.message("emr.toolwindow.title")

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    EmrToolWindowController.getInstance(project)?.setUp(toolWindow)
  }
}