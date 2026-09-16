package com.jetbrains.spark.monitoring.ui.pages

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractGroupFieldsModelsController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.spark.monitoring.data.AppAttemptId
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.settings.SparkToolwindowSettings
import com.jetbrains.spark.monitoring.ui.utils.OpenUrlAction

class EnvironmentPageController(project: Project, val connectionId: String) :
  AbstractGroupFieldsModelsController<AppAttemptId>(project, connectionId), DetailsMonitoringController<AppAttemptId> {

  override val dataManager = SparkDataManager.getInstance(connectionId, project) ?: error("Data Manager is not initialized")
  override val toolWindowSettings = SparkToolwindowSettings.getInstance()

  init {
    init()
  }

  override fun getFieldsGroupModel(id: AppAttemptId) = dataManager.getEnvironmentModel(id)

  override fun createActions(): List<AnAction> {
    return listOf(OpenUrlAction({ "environment" }, project, dataManager) { id?.toString() })
  }
}