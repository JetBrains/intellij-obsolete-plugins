package com.jetbrains.bigdatatools.glue.monitoring.controllers

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterAdapter
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.table.TableClickHelper
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableColumnsFitter
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableLoadingDecorator
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TableWithDetailsMonitoringController
import com.jetbrains.bigdatatools.common.table.renderers.LinkRenderer
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.bigdatatools.common.ui.ToolbarLabelActionImpl
import com.jetbrains.bigdatatools.common.ui.filter.CountFilterPopupComponent
import com.jetbrains.bigdatatools.glue.client.GlueDataManager
import com.jetbrains.bigdatatools.glue.monitoring.models.DatabaseId
import com.jetbrains.bigdatatools.glue.monitoring.models.GlueTableInfo
import com.jetbrains.bigdatatools.glue.monitoring.models.TableId
import com.jetbrains.bigdatatools.glue.settings.GlueToolWindowSettings
import com.jetbrains.bigdatatools.glue.utils.GlueMessagesBundle
import java.util.EnumSet

class GlueTableController(val project: Project,
                          private val dataManager: GlueDataManager) : TableWithDetailsMonitoringController<GlueTableInfo, String>(),
                                                                      DetailsMonitoringController<String> {
  override val detailsController = GlueTableTabsController(project, dataManager)

  private var selectedId: String? = null

  init {
    init()
  }

  override fun getColumnSettings() = GlueToolWindowSettings.getInstance().tableSettings

  override fun getRenderableColumns() = GlueTableInfo.renderableColumns

  override fun getDataModel() = selectedId?.let { dataManager.getTablesModel(DatabaseId.fromString(it)) }

  override fun getTableExtensions(): EnumSet<TableExtensionType> =
    EnumSet.copyOf(super.getTableExtensions() - TableExtensionType.LOADING_INDICATOR)

  override fun getToolbarTitle() = GlueMessagesBundle.message("block.tables")

  override fun setDetailsId(id: String) {
    selectedId = id

    val model = getDataModel() ?: return
    dataTable.tableModel.setDataModel(model)

    TableColumnsFitter.get(dataTable)?.reset()
    TableLoadingDecorator.installOn(dataTable)

    decoratedTableComponent.revalidate()
    decoratedTableComponent.repaint()
  }

  override fun indexToDetailId(row: Int): String? {
    val tableInfo = dataTable.getDataAt(row) ?: return null
    return TableId(tableInfo.catalog, tableInfo.database, tableInfo.name).toString()
  }

  override fun saveSelectedItem() = Unit

  override fun createTopLeftToolbarActions(): List<AnAction> {
    val settings = GlueToolWindowSettings.getInstance()
    val config = settings.getOrCreateConfig(dataManager.connectionId)

    val userText = JBTextField(config.tablePattern, 8)

    FilterAdapter.install(dataTable.tableModel, userText, GlueTableInfo.TEXT_FILTER) { userQuery ->
      config.tablePattern = userQuery
      reloadTables()
    }

    val countFilter = CountFilterPopupComponent(GlueMessagesBundle.message("filter.limit"), config.tableLimit)
    FilterAdapter.install(dataTable.tableModel, countFilter, GlueTableInfo.LIMIT_FILTER) { limit ->
      config.tableLimit = limit
      reloadTables()
    }

    return listOf(ToolbarLabelActionImpl(GlueMessagesBundle.message("filter.text")),
                  CustomComponentActionImpl(userText),
                  CustomComponentActionImpl(countFilter))
  }

  override fun customTableInit(table: DataTable<GlueTableInfo>) {
    LinkRenderer.installOnColumn(table, columnModel.getColumn(2)).apply {
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
      dataManager.updater.invokeRefreshModel(dataManager.getTablesModel(DatabaseId.fromString(it)))
    }
  }
}