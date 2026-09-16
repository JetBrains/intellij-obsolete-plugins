package com.jetbrains.hadoop.monitoring.action

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.hadoop.monitoring.data.HadoopDataManager
import com.jetbrains.hadoop.monitoring.statistics.HadoopMonitoringUsagesCollector
import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle

class OpenUrlAction(private val suffixProvider: (() -> String),
                    private val project: Project,
                    private val dataManager: HadoopDataManager)
  : DumbAwareAction(MessagesBundle.message("open.in.browser.title"),
                    HadoopMessagesBundle.message("open.url.tooltip"),
                    AllIcons.General.Web) {

  override fun actionPerformed(e: AnActionEvent) {
    HadoopMonitoringUsagesCollector.openedInBrowserEvent.log(project)
    BrowserUtil.browse("${dataManager.getRealUrl().removeSuffix("/")}/${suffixProvider.invoke()}")
  }
}