package com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobs

import com.intellij.openapi.actionSystem.AnAction
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsTableMonitoringController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.model.JobExceptionEntry
import com.jetbrains.bigdatatools.flink.toolwindow.config.FlinkToolWindowSettings

class FlinkJobExceptionHistoryController(private val dataManager: FlinkDataManager) : DetailsTableMonitoringController<JobExceptionEntry, String>() {
  init {
    init()
  }

  override fun getColumnSettings() = FlinkToolWindowSettings.getInstance().jobExceptionHistorySettings

  override fun getRenderableColumns() = JobExceptionEntry.renderableColumns

  override fun getDataModel() = selectedId?.let { dataManager.getJobExceptionHistoryModel(it) }

  override fun showColumnFilter(): Boolean = false

  override fun getAdditionalActions(): List<AnAction> = listOf(
    OpenUrlAction(dataManager) {
      val jobId = selectedId ?: return@OpenUrlAction null
      "/#/job/$jobId/exceptions"
    }
  )
}