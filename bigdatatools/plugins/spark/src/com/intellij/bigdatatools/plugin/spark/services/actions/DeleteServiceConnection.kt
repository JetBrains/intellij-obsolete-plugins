package com.intellij.bigdatatools.plugin.spark.services.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.util.ConnectionUtil

class DeleteServiceConnection : ServiceConnectionAction() {
  override fun update(e: AnActionEvent) {
    if (!isFromTree(e) || !isRootDriver(e)) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    val selectedConnectionIds = getSelectedConnectionIds(e)
    e.presentation.isEnabled = selectedConnectionIds.isNotEmpty()
    e.presentation.text = MessagesBundle.message("action.deleteConnection.text", if (selectedConnectionIds.size == 1) 0 else 1)
  }

  override fun actionPerformed(e: AnActionEvent) {
    if (!isFromTree(e)) {
      return
    }

    val project = e.project ?: return
    val selectedConnectionIds = getSelectedConnectionIds(e)
    if (selectedConnectionIds.isEmpty()) return
    ConnectionUtil.removeConnectionsWithConfirmation(project, selectedConnectionIds)
  }
}