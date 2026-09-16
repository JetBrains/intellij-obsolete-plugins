package com.jetbrains.spark.monitoring.ui.pages

import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsTableMonitoringController
import com.jetbrains.spark.monitoring.data.AppAttemptId
import com.jetbrains.spark.monitoring.data.ExecutorsAggregateInfo
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.settings.SparkToolwindowSettings
import com.jetbrains.spark.monitoring.util.SMMessagesBundle

class SparkAggregateExecutorsController(private val dataManager: SparkDataManager) : DetailsTableMonitoringController<ExecutorsAggregateInfo, AppAttemptId>() {
  init {
    init()
  }

  override fun getColumnSettings() = SparkToolwindowSettings.getInstance().executorsAggregateColumnSettings
  override fun getRenderableColumns() = ExecutorsAggregateInfo.renderableColumns

  override fun getDataModel(): ObjectDataModel<ExecutorsAggregateInfo>? {
    val applicationId = selectedId ?: return null
    return dataManager.getExecutorsAggregateModel(applicationId)
  }

  override fun showColumnFilter(): Boolean = true

  override fun getToolbarTitle() = SMMessagesBundle.message("applications.tab.details")
}