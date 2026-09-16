package com.intellij.bigdatatools.zeppelin.idea.toolwindow.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings
import com.jetbrains.bigdatatools.common.util.BdtActionsBundle

@Suppress("DialogTitleCapitalization")
class OpenGlobalSettingsFormAction(val configId: String?) :
  DumbAwareAction(BdtActionsBundle.message("action.BigDataTools.RfsOpenSettingsAction.text"),
                  BdtActionsBundle.message("action.BigDataTools.RfsOpenSettingsAction.description"),
                  AllIcons.General.Settings) {
  override fun actionPerformed(e: AnActionEvent) {
    val project: Project = e.project ?: return
    ConnectionSettings.open(project, configId)
  }
}