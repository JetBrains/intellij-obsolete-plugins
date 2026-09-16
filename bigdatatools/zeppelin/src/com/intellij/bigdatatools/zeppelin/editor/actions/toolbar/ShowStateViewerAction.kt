package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.zeppelin.idea.toolwindow.ZtoolsToolWindowUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class ShowStateViewerAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    ZtoolsToolWindowUtils.showToolWindow(project)
  }
}