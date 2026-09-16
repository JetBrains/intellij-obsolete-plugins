package com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobmanager

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractTableController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.model.JobManagerConfig
import com.jetbrains.bigdatatools.flink.toolwindow.config.FlinkToolWindowSettings

class FlinkJobManagerConfigController(val project: Project,
                                      private val dataManager: FlinkDataManager) : AbstractTableController<JobManagerConfig>() {
  init {
    init()
  }

  override fun getColumnSettings() = FlinkToolWindowSettings.getInstance().jobManagerConfigSettings

  override fun getRenderableColumns() = JobManagerConfig.renderableColumns

  override fun getDataModel() = dataManager.jobManagerConfigModel

  override fun showColumnFilter(): Boolean = false

  override fun getAdditionalActions(): List<AnAction> = listOf(OpenUrlAction(dataManager) { "/#/job-manager/config" } )
}