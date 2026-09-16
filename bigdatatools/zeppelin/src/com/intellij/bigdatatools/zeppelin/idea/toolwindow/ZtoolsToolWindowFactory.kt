package com.intellij.bigdatatools.zeppelin.idea.toolwindow

import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

/**
 * Factory that creates a Zeppelin tool window
 */
class ZtoolsToolWindowFactory : ToolWindowFactory, DumbAware {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val contentManager = toolWindow.contentManager

    contentManager.addContent(ZtoolsToolWindowUtils.createEmptyContent(contentManager))

    contentManager.addUiDataProvider { sink ->
      sink[PlatformDataKeys.HELP_ID] = "big.data.tools.notebooks.running.stateviewer"
    }
  }

  override fun init(toolWindow: ToolWindow) {
    toolWindow.stripeTitle = ZepMessagesBundle.message("toolwindow.title")
  }

  override fun shouldBeAvailable(project: Project) = false

  companion object {
    const val ID: String = "zeppelin-shell-toolwindow"
  }
}