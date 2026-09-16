package com.intellij.bigdatatools.plugin.spark.services.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.bigdatatools.common.util.ConnectionUtil

class RefreshServiceConnection : ServiceConnectionAction() {
  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedConnectionIds = getSelectedConnectionIds(e)
    val selectedAndEnabledNodes = selectedConnectionIds.filter {
      RfsConnectionDataManager.instance?.getConnectionById(project, it)?.isEnabled == true
    }
    e.presentation.isEnabledAndVisible = selectedAndEnabledNodes.isNotEmpty()
    if (e.presentation.isVisible) {
      e.presentation.text = MessagesBundle.message("action.refreshConnection.text", if (selectedConnectionIds.size == 1) 0 else 1)
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedConnectionIds = getSelectedConnectionIds(e)
    if (selectedConnectionIds.isEmpty())
      return

    ConnectionUtil.refreshConnectionsByIds(project, selectedConnectionIds)
  }
}