package com.jetbrains.bigdatatools.hivemetastore.monitoring.controllers

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsGroupModel
import com.jetbrains.bigdatatools.common.monitoring.list.ListClickHelper
import com.jetbrains.bigdatatools.common.monitoring.list.ListValueRenderer
import com.jetbrains.bigdatatools.common.monitoring.list.model.ListTableModel
import com.jetbrains.bigdatatools.common.monitoring.table.TableClickHelper
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractGroupFieldsModelsController
import com.jetbrains.bigdatatools.common.rfs.url.UrlDriverChooserAction
import com.jetbrains.bigdatatools.common.rfs.util.withSlash
import com.jetbrains.bigdatatools.common.table.renderers.LinkRenderer
import com.jetbrains.bigdatatools.common.table.search.SearchAwareMaterialTable
import com.jetbrains.bigdatatools.common.ui.BdtJsonInfoDialog
import com.jetbrains.bigdatatools.hivemetastore.client.HiveDataManager
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HiveTableInfo
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.TableId
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveToolWindowSettings
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMessagesBundle
import kotlin.math.min
import kotlin.reflect.KProperty1

class HiveTableSummaryController(project: Project,
                                 override val dataManager: HiveDataManager) :
  AbstractGroupFieldsModelsController<String>(project,
                                              dataManager.client.connectionData.innerId) {

  override val toolWindowSettings = HiveToolWindowSettings.getInstance()

  init {
    init()
  }

  override fun getFieldsGroupModel(id: String): FieldsGroupModel<HiveTableInfo> {
    val tableId = id.let { TableId.fromString(it) }
    return dataManager.getTableSummary(tableId)
  }

  override fun createTable(renderableColumns: List<KProperty1<out Any, *>>,
                           tableModel: ListTableModel): SearchAwareMaterialTable {
    val table = super.createTable(renderableColumns, tableModel)

    val locationLabel = HiveMessagesBundle.message("meta.label.location")

    if (table.columnModel.columnCount > 1) {
      table.columnModel.getColumn(1).cellRenderer = ListValueRenderer(mapOf(locationLabel to LinkRenderer()))
    }

    ListClickHelper.installOn(table, locationLabel) {
      val url = it as? String ?: return@installOn

      var foundRow = -1
      for (i in 0 until table.rowCount) {
        if (table.getValueAt(i, 0) == locationLabel) {
          foundRow = i
          break
        }
      }

      TableClickHelper.showPopupUnderCell(UrlDriverChooserAction(project, url.withSlash()), table,
                                          min(foundRow, table.rowCount),
                                          min(1, table.columnCount))
    }

    return table
  }

  override fun createActions(): List<AnAction> {
    val showClusterDetailsAction = object : DumbAwareAction(HiveMessagesBundle.message("action.show.table.details"), null,
                                                            AllIcons.FileTypes.Json) {
      override fun actionPerformed(e: AnActionEvent) {
        val tableId = id?.let { TableId.fromString(it) } ?: return
        val table = dataManager.getTable(tableId.catalog, tableId.database, tableId.table) ?: return

        BdtJsonInfoDialog(project, table.tableName, table).show()
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = id != null
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    return super.createActions() + listOf(showClusterDetailsAction)
  }
}