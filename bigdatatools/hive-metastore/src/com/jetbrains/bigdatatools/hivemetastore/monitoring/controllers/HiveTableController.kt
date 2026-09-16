package com.jetbrains.bigdatatools.hivemetastore.monitoring.controllers

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.SmartPopupActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.jetbrains.bigdatatools.common.monitoring.data.model.DataModelFilter
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
import com.jetbrains.bigdatatools.hivemetastore.client.HiveDataManager
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.DatabaseId
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HiveTableInfo
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.TableId
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveToolWindowSettings
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMessagesBundle
import org.apache.hadoop.hive.metastore.TableType
import java.util.EnumSet

class HiveTableController(val project: Project,
                          private val dataManager: HiveDataManager) : TableWithDetailsMonitoringController<HiveTableInfo, String>(),
                                                                      DetailsMonitoringController<String> {
  override val detailsController = HiveTableTabsController(project, dataManager)

  private var selectedId: String? = null

  init {
    init()
  }

  override fun getColumnSettings() = HiveToolWindowSettings.getInstance().tableSettings

  override fun getRenderableColumns() = HiveTableInfo.renderableColumns

  override fun getDataModel() = selectedId?.let { dataManager.getDatabaseModel(DatabaseId.fromString(it)) }

  override fun getTableExtensions(): EnumSet<TableExtensionType> =
    EnumSet.copyOf(super.getTableExtensions() - TableExtensionType.LOADING_INDICATOR)

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
    val statusFilter = object : SmartPopupActionGroup() {
      override fun isDumbAware(): Boolean = true
    }

    statusFilter.templatePresentation.text = HiveMessagesBundle.message("filter.table.type")
    statusFilter.templatePresentation.icon = AllIcons.General.Filter

    val settings = HiveToolWindowSettings.getInstance()

    for (tableType in TableType.entries) {
      @Suppress("HardCodedStringLiteral")
      val text = tableType.name

      val toggleState = object : DumbAwareToggleAction(text, text, null) {
        override fun isSelected(e: AnActionEvent) = settings.filterTableTypes.contains(tableType.name)
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) {
          val filters = dataTable.tableModel.getDataModel()?.filters

          if (state) {
            settings.filterTableTypes.add(tableType.name)
            filters?.setFilter(DataModelFilter(HiveTableInfo.TYPE_FILTER, settings.filterTableTypes.joinToString(",")))
          }
          else {
            settings.filterTableTypes.remove(tableType.name)
            if (settings.filterTableTypes.isEmpty()) {
              filters?.removeFilter(HiveTableInfo.TYPE_FILTER)
            }
            else {
              filters?.setFilter(DataModelFilter(HiveTableInfo.TYPE_FILTER, settings.filterTableTypes.joinToString(",")))
            }
          }
          reloadTables()
        }
      }

      statusFilter.add(toggleState)
    }

    val config = settings.getOrCreateConfig(dataManager.connectionId)

    val userText = JBTextField(config.tablePattern, 8)

    FilterAdapter.install(dataTable.tableModel, userText, HiveTableInfo.TEXT_FILTER) { userQuery ->
      config.tablePattern = userQuery
      reloadTables()
    }

    return listOf(ToolbarLabelActionImpl(HiveMessagesBundle.message("filter.text")),
                  CustomComponentActionImpl(userText),
                  statusFilter)
  }

  override fun customTableInit(table: DataTable<HiveTableInfo>) {
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
      dataManager.updater.invokeRefreshModel(dataManager.getDatabaseModel(DatabaseId.fromString(it)))
    }
  }
}