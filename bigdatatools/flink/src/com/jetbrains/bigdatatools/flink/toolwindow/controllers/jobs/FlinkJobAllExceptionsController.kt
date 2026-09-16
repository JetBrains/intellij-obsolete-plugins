package com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobs

import com.intellij.openapi.actionSystem.AnAction
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsTableMonitoringController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.model.JobExceptionType
import com.jetbrains.bigdatatools.flink.toolwindow.config.FlinkToolWindowSettings

class FlinkJobAllExceptionsController(private val dataManager: FlinkDataManager) : DetailsTableMonitoringController<JobExceptionType, String>() {
  init {
    init()
  }

  override fun getColumnSettings() = FlinkToolWindowSettings.getInstance().jobAllExceptionsSettings

  override fun getRenderableColumns() = JobExceptionType.renderableColumns

  override fun getDataModel() = selectedId?.let { dataManager.getJobAllExceptionsModel(it) }

  override fun showColumnFilter(): Boolean = false

  override fun getAdditionalActions(): List<AnAction> = listOf(
    OpenUrlAction(dataManager) {
      val jobId = selectedId ?: return@OpenUrlAction null
      "/#/job/$jobId/exceptions"
    }
  )
}