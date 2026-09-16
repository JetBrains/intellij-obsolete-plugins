package com.jetbrains.bigdatatools.glue.monitoring.controllers

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterAdapter
import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.table.TableClickHelper
import com.jetbrains.bigdatatools.common.monitoring.table.getSelectedData
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsTableMonitoringController
import com.jetbrains.bigdatatools.common.table.renderers.LinkRenderer
import com.jetbrains.bigdatatools.common.ui.BdtJsonInfoDialog
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.bigdatatools.common.ui.filter.CountFilterPopupComponent
import com.jetbrains.bigdatatools.glue.client.GlueDataManager
import com.jetbrains.bigdatatools.glue.monitoring.models.GluePartitionInfo
import com.jetbrains.bigdatatools.glue.monitoring.models.TableId
import com.jetbrains.bigdatatools.glue.settings.GlueToolWindowSettings
import com.jetbrains.bigdatatools.glue.utils.GlueMessagesBundle

class GlueTablePartitionsController(val project: Project,
                                    private val dataManager: GlueDataManager) : DetailsTableMonitoringController<GluePartitionInfo, String>() {
  init {
    init()
  }

  override fun getColumnSettings() = GlueToolWindowSettings.getInstance().partitionSettings

  override fun getRenderableColumns() = GluePartitionInfo.renderableColumns

  override fun showColumnFilter() = false

  override fun getAdditionalActions(): List<AnAction> {
    val detailsAction = object : DumbAwareAction(GlueMessagesBundle.message("action.show.partition.details"), null,
                                                 AllIcons.FileTypes.Json) {
      override fun actionPerformed(e: AnActionEvent) {
        val partition = getPartition() ?: return
        BdtJsonInfoDialog(project, partition.name, partition.partition).show()
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getPartition() != null
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }
    return listOf(detailsAction)
  }

  private fun getPartition(): GluePartitionInfo? = dataTable.getSelectedData()

  override fun createTopLeftToolbarActions(): List<AnAction> {
    val settings = GlueToolWindowSettings.getInstance()

    val config = settings.getOrCreateConfig(dataManager.connectionId)

    val countFilter = CountFilterPopupComponent(GlueMessagesBundle.message("filter.limit"), config.partitionLimit)
    FilterAdapter.install(dataTable.tableModel, countFilter, GluePartitionInfo.LIMIT_FILTER) { limit ->
      config.partitionLimit = limit
      reloadTables()
    }

    return listOf(CustomComponentActionImpl(countFilter))
  }

  override fun getDataModel(): ObjectDataModel<GluePartitionInfo>? {
    val tableId = selectedId?.let { TableId.fromString(it) } ?: return null
    return dataManager.getPartitionsModel(tableId)
  }

  override fun customTableInit(table: DataTable<GluePartitionInfo>) {
    LinkRenderer.installOnColumn(table, columnModel.getColumn(1)).apply {
      condition = { _, _, row, _ ->
        table.getDataAt(row)?.location?.isNotBlank() == true
      }

      onClick = { row, column ->
        showConnection(row, column)
      }
    }
  }

  private fun showConnection(row: Int, column: Int) {
    if (row == -1)
      return

    TableClickHelper.showPopupUnderCell(project, dataTable.getDataAt(row)?.location, dataTable, row, column)
  }

  private fun reloadTables() {
    selectedId?.let {
      dataManager.updater.invokeRefreshModel(dataManager.getPartitionsModel(TableId.fromString(it)))
    }
  }
}