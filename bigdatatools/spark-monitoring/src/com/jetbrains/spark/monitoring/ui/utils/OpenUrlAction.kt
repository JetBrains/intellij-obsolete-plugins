package com.jetbrains.spark.monitoring.ui.utils

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.statistics.SparkMonitoringUsagesCollector
import com.jetbrains.spark.monitoring.statistics.ToolbarActionType
import com.jetbrains.spark.monitoring.util.SMMessagesBundle

open class OpenUrlAction(private val suffixProvider: (() -> String),
                         private val project: Project,
                         private val dataManager: SparkDataManager,
                         private val applicationIdProvider: (() -> String?))
  : DumbAwareAction(MessagesBundle.message("open.in.browser.title"), SMMessagesBundle.message("open.url.tooltip"), AllIcons.General.Web) {
  override fun actionPerformed(e: AnActionEvent) {
    val realUrl = dataManager.getRealUrl()
    if (dataManager.connectionData.historyServer) {
      BrowserUtil.browse("$realUrl/history/${applicationIdProvider.invoke()}/${suffixProvider.invoke()}")
    }
    else {
      BrowserUtil.browse("$realUrl/${suffixProvider.invoke()}")
    }

    SparkMonitoringUsagesCollector.toolbarActionInvokedEvent.log(project, ToolbarActionType.OpenUrl)
  }
}