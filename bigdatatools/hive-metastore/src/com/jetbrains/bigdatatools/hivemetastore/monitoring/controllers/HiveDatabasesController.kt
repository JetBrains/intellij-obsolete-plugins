package com.jetbrains.bigdatatools.hivemetastore.monitoring.controllers

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterAdapter
import com.jetbrains.bigdatatools.common.monitoring.table.getSelectedData
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TableWithDetailsMonitoringController
import com.jetbrains.bigdatatools.common.ui.BdtJsonInfoDialog
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.bigdatatools.common.ui.ToolbarLabelActionImpl
import com.jetbrains.bigdatatools.hivemetastore.client.HiveDataManager
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.DatabaseId
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HiveDatabaseInfo
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveMetastoreConnectionData
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveToolWindowSettings
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMessagesBundle
import org.apache.hadoop.hive.metastore.api.Database

class HiveDatabasesController(val project: Project,
                              connectionData: HiveMetastoreConnectionData) : TableWithDetailsMonitoringController<HiveDatabaseInfo, String>() {
  private val dataManager = HiveDataManager.getInstance(connectionData.innerId, project) ?: error("DataManager is not inited")

  override val detailsController = HiveTableController(project, dataManager)

  init {
    init()
  }

  override fun showColumnFilter() = false

  override fun getColumnSettings() = HiveToolWindowSettings.getInstance().databaseSettings

  override fun getRenderableColumns() = HiveDatabaseInfo.renderableColumns

  override fun getDataModel() = dataManager.rootModel

  override fun indexToDetailId(row: Int): String? {
    val infoAt = dataTable.getDataAt(row) ?: return null
    return DatabaseId(infoAt.catalog, infoAt.name).toString()
  }

  override fun saveSelectedItem() = Unit

  override fun getAdditionalActions(): List<AnAction> {
    val detailsAction = object : DumbAwareAction(HiveMessagesBundle.message("action.show.database.details"), null,
                                                 AllIcons.FileTypes.Json) {
      override fun actionPerformed(e: AnActionEvent) {
        val database = getDatabase() ?: return
        BdtJsonInfoDialog(project, database.name, database).show()
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getDatabase() != null
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT

      private fun getDatabase(): Database? = dataTable.getSelectedData()?.database
    }

    return listOf(detailsAction)
  }

  override fun createTopLeftToolbarActions(): List<AnAction> {
    val settings = HiveToolWindowSettings.getInstance()
    val config = settings.getOrCreateConfig(dataManager.connectionId)

    val userText = JBTextField(config.databasePattern, 8)

    FilterAdapter.install(dataTable.tableModel, userText, HiveDatabaseInfo.TEXT_FILTER) { userQuery ->
      config.databasePattern = userQuery
      reloadTables()
    }

    return listOf(ToolbarLabelActionImpl(HiveMessagesBundle.message("filter.text")), CustomComponentActionImpl(userText))
  }

  private fun reloadTables() {
    dataManager.updater.invokeRefreshModel(dataManager.rootModel)
  }
}