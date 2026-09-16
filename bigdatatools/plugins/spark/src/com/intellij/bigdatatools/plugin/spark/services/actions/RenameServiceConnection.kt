package com.intellij.bigdatatools.plugin.spark.services.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.bigdatatools.common.util.ConnectionUtil

class RenameServiceConnection : ServiceConnectionAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedConnectionIds = getSelectedConnectionIds(e)
    if (selectedConnectionIds.isEmpty()) return
    ConnectionUtil.renameConnection(project, selectedConnectionIds.first())
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isFromTree(e) && isRootDriver(e)
  }
}