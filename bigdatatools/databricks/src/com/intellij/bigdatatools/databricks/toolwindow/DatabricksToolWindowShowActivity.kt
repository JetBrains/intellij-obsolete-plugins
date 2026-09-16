package com.intellij.bigdatatools.databricks.toolwindow

import com.intellij.bigdatatools.databricks.toolwindow.config.DatabricksToolWindowSettings
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.ExperimentalUI
import com.jetbrains.bigdatatools.common.constants.BdtPlugins
import com.jetbrains.bigdatatools.common.util.invokeLater

/**
 * New UI does not open our Toolwindow after plugin installation. The idea of IDEA is to "minimize user distraction".
 * But our user will not find our functionality with closed, and that's why we're opening toolwindow first time programmatically.
 */
class DatabricksToolWindowShowActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (BdtPlugins.isFullPluginInstalled()) return

    // Only on new UI we need to do this tricks with force showing of the ToolWindow.
    if (!ExperimentalUI.isNewUI()) return
    val pluginVersion = PluginManagerCore.getPlugin(PluginId.getId(BdtPlugins.DATABRICKS_ID))?.version
                        ?: return
    val lastShownVersion = DatabricksToolWindowSettings.getInstance().lastShownToolWindowVersion ?: ""
    if (lastShownVersion == pluginVersion) return

    val toolwindow = ToolWindowManager.getInstance(project).getToolWindow(DatabricksMonitoringToolWindowController.TOOL_WINDOW_ID) ?: return
    if (!toolwindow.isVisible) {
      invokeLater {
        if (toolwindow.isDisposed)
          return@invokeLater
        toolwindow.show()
      }
    }
    DatabricksToolWindowSettings.getInstance().lastShownToolWindowVersion = pluginVersion
  }
}