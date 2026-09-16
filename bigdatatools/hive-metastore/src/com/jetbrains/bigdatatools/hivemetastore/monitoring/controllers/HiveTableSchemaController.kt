package com.jetbrains.bigdatatools.hivemetastore.monitoring.controllers

import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsTableMonitoringController
import com.jetbrains.bigdatatools.hivemetastore.client.HiveDataManager
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HiveSchemaInfo
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.TableId
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveToolWindowSettings

class HiveTableSchemaController(private val dataManager: HiveDataManager) : DetailsTableMonitoringController<HiveSchemaInfo, String>() {
  init {
    init()
  }

  override fun getColumnSettings() = HiveToolWindowSettings.getInstance().schemaSettings

  override fun getRenderableColumns() = HiveSchemaInfo.renderableColumns

  override fun showColumnFilter() = false

  override fun getDataModel(): ObjectDataModel<HiveSchemaInfo>? {
    val tableId = selectedId?.let { TableId.fromString(it) } ?: return null
    return dataManager.getSchemaModel(tableId)
  }
}