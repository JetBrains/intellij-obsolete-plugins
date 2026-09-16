package com.intellij.bigdatatools.databricks.toolwindow

import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.constants.BdtPlugins
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowFactory
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager

class DatabricksToolWindowFactory : MonitoringToolWindowFactory() {
  override val toolWindowId: String = DatabricksMonitoringToolWindowController.TOOL_WINDOW_ID
  override val connectionType = BdtConnectionType.DATABRICKS
  override val title: String = DatabricksBundle.message("toolwindow.title")

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    DatabricksMonitoringToolWindowController.getInstance(project)?.setUp(toolWindow)
  }

  override fun shouldBeAvailable(project: Project): Boolean {
    // We will show ToolWindow stripe button if:
    // 1. Only separate Kafka plugin installed
    // 2. Full BDT installed, and we have any Databricks connection configured.
    return (BdtPlugins.isDatabricksPluginInstalled() && !BdtPlugins.isFullPluginInstalled()) ||
           (BdtPlugins.isFullPluginInstalled() &&
            !RfsConnectionDataManager.instance?.getConnectionsByGroupId(connectionType.id, project).isNullOrEmpty())
  }

}