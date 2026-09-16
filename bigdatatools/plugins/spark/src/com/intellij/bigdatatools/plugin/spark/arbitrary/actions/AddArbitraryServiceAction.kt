package com.intellij.bigdatatools.plugin.spark.arbitrary.actions

import com.intellij.bigdatatools.plugin.spark.arbitrary.ArbitraryClusterConnectionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings

class AddArbitraryServiceAction : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    ConnectionSettings.create(project, ArbitraryClusterConnectionGroup(), applyIfOk = true)
  }
}