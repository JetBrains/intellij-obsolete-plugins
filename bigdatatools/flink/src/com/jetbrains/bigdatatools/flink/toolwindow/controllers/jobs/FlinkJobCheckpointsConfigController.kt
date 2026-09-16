package com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobs

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractGroupFieldsModelsController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.toolwindow.config.FlinkToolWindowSettings

class FlinkJobCheckpointsConfigController(project: Project, override val dataManager: FlinkDataManager) :
  AbstractGroupFieldsModelsController<String>(project, dataManager.connectionData.innerId) {

  override val toolWindowSettings = FlinkToolWindowSettings.getInstance()

  init {
    init()
  }

  override fun getFieldsGroupModel(id: String) = dataManager.getJobCheckpointsConfigModel(id)

  override fun createActions(): List<AnAction> {
    return super.createActions() + listOf(OpenUrlAction(dataManager) {
      val jobId = id ?: return@OpenUrlAction null
      "/#/job/$jobId/checkpoints"
    })
  }
}