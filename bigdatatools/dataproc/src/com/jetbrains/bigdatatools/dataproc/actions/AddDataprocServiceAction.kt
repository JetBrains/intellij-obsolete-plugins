package com.jetbrains.bigdatatools.dataproc.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings
import com.jetbrains.bigdatatools.dataproc.settings.DataprocConnectionGroup

// Used in Services ToolWindow. Enabled and visible only when integration with services enabled.
class AddDataprocServiceAction : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    ConnectionSettings.create(project, DataprocConnectionGroup(), applyIfOk = true)
  }
}