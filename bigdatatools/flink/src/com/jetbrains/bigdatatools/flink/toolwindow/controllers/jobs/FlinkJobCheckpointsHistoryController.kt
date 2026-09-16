package com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobs

import com.intellij.openapi.actionSystem.AnAction
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsTableMonitoringController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.model.JobCheckpointsHistory
import com.jetbrains.bigdatatools.flink.toolwindow.config.FlinkToolWindowSettings

class FlinkJobCheckpointsHistoryController(val dataManager: FlinkDataManager) : DetailsTableMonitoringController<JobCheckpointsHistory, String>() {
  init {
    init()
  }

  override fun getColumnSettings() = FlinkToolWindowSettings.getInstance().jobCheckpointHistorySettings

  override fun getDataModel() = selectedId?.let { dataManager.getJobCheckpointsHistoryModel(it) }

  override fun getRenderableColumns() = JobCheckpointsHistory.renderableColumns

  override fun showColumnFilter(): Boolean = false

  override fun getAdditionalActions(): List<AnAction> = listOf(
    OpenUrlAction(dataManager) {
      val jobId = selectedId ?: return@OpenUrlAction null
      "/#/job/$jobId/checkpoints"
    }
  )
}