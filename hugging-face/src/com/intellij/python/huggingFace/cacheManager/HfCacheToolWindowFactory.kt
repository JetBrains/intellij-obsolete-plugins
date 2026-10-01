package com.intellij.python.huggingFace.cacheManager

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.python.huggingFace.cacheManager.ui.HfCacheViewPanel

class HfCacheToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun init(toolWindow: ToolWindow) {
    HfCachePackageManagementListenerService.getInstance(toolWindow.project).register(toolWindow)
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val panel = HfCacheViewPanel(project)
    val content = toolWindow.contentManager.factory.createContent(panel, null, false)
    toolWindow.contentManager.addContent(content)
  }

  override suspend fun isApplicableAsync(project: Project): Boolean {
    HfCachePackageManagementListenerService.getInstance(project).createListener()
    return true
  }

  override fun shouldBeAvailable(project: Project): Boolean = false
}