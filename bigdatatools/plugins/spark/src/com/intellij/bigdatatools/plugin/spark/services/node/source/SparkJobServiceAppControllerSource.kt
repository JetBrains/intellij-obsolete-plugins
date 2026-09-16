package com.intellij.bigdatatools.plugin.spark.services.node.source

import com.intellij.bigdatatools.plugin.spark.services.SparkJobServiceViewContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.rfs.driver.ActivitySource
import com.jetbrains.bigdatatools.common.rfs.driver.refreshConnectionLaunch
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings
import com.jetbrains.bigdatatools.common.ui.AutorefreshPopupComponent
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.spark.monitoring.data.PresentableApplicationInfo
import com.jetbrains.spark.monitoring.rfs.driver.SparkMonitoringDriver
import com.jetbrains.spark.monitoring.settings.SparkToolwindowSettings
import com.jetbrains.spark.monitoring.ui.pages.SparkAppControllerSource
import javax.swing.BorderFactory

class SparkJobServiceAppControllerSource(val driver: SparkMonitoringDriver) : SparkAppControllerSource {
  private val settings = SparkToolwindowSettings.getInstance()
  private val dataManager = driver.dataManager
  private val updater = dataManager.updater

  override val rightToolbarActions: List<AnAction> = let {
    val settingsAction = object : DumbAwareAction(MessagesBundle.message("open.settings.action.text"),
                                                  MessagesBundle.message("open.settings.action.description"),
                                                  AllIcons.General.Settings) {
      init {
        templatePresentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
      }

      override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ConnectionSettings.open(project, driver.connectionData.innerId)
      }
    }

    val autoRefresh = AutorefreshPopupComponent().apply {
      isOpaque = false
      value = settings.dataUpdateIntervalMillis
      border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
      onRefreshIntervalChanged = ::onRefreshIntervalChanged
      onActionPerformed = ::onRefreshAction
    }

    settings.delegateTimeUpdate.plusAssign {
      autoRefresh.value = it
    }

    val progressComponent = dataManager.progressComponent
    listOf(CustomComponentActionImpl(progressComponent.component),
           CustomComponentActionImpl(autoRefresh),
           Separator.create(),
           settingsAction)
  }

  private fun onRefreshIntervalChanged(newInterval: Int) {
    settings.dataUpdateIntervalMillis = newInterval
    if (newInterval <= 0)
      updater.stopAll()
    else
      updater.rescheduleAll()
  }

  private fun onRefreshAction(@Suppress("UNUSED_PARAMETER") e: AnActionEvent) {
    driver.refreshConnectionLaunch(ActivitySource.ACTION)
  }

  override fun open(project: Project, info: PresentableApplicationInfo) {
    SparkJobServiceViewContributor.Utils.getInstance()?.focusOnSparkApp(project, driver.connectionData, info, ignoreIfError = false)
  }
}