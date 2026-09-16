package com.intellij.bigdatatools.emr.actions

import com.intellij.bigdatatools.emr.settings.EmrConnectionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings

// Used in Services ToolWindow. Enabled and visible only when integration with services enabled.
class AddEmrServiceAction : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    ConnectionSettings.create(project, EmrConnectionGroup(), applyIfOk = true)
  }
}