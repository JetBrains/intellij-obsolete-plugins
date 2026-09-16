package com.jetbrains.spark.monitoring.ui.pages

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractGroupFieldsModelsController
import com.jetbrains.spark.monitoring.data.AppAttemptId
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.settings.SparkToolwindowSettings

class SparkApplicationInfoController(project: Project, connectionId: String) : AbstractGroupFieldsModelsController<AppAttemptId>(project,
                                                                                                                                 connectionId) {

  override val dataManager = SparkDataManager.getInstance(connectionId, project) ?: error("Data Manager is not initialized")
  override val toolWindowSettings = SparkToolwindowSettings.getInstance()

  init {
    init()
  }

  override fun getFieldsGroupModel(id: AppAttemptId) = dataManager.applicationInfos[id]
}