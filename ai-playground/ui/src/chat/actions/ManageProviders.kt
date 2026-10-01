package com.intellij.aiplayground.ui.chat.actions

import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class ManageProviders : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    PlaygroundCollector.logManageProvidersOpened()
    ShowSettingsUtilImpl.showSettingsDialog(
      e.project,
      "com.intellij.aiplayground.settings.llm",
      null
    )
  }
}