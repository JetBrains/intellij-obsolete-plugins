package com.jetbrains.bigdatatools.glue.monitoring.controllers

import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsTableMonitoringController
import com.jetbrains.bigdatatools.glue.client.GlueDataManager
import com.jetbrains.bigdatatools.glue.monitoring.models.GlueColumnInfo
import com.jetbrains.bigdatatools.glue.monitoring.models.TableId
import com.jetbrains.bigdatatools.glue.settings.GlueToolWindowSettings

class GlueTableSchemaController(private val dataManager: GlueDataManager) :
  DetailsTableMonitoringController<GlueColumnInfo, String>() {

  init {
    init()
  }

  // To show empty vertical toolbar which will separate Schema data from other tables.
  override fun getToolbarTitle() = ""

  override fun getColumnSettings() = GlueToolWindowSettings.getInstance().schemaSettings

  override fun getRenderableColumns() = GlueColumnInfo.renderableColumns

  override fun showColumnFilter() = false

  override fun getDataModel(): ObjectDataModel<GlueColumnInfo>? {
    val tableId = selectedId?.let { TableId.fromString(it) } ?: return null
    return dataManager.getSchemaModel(tableId)
  }
}