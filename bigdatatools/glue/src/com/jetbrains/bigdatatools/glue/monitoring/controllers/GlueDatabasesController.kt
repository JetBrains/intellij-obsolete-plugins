package com.jetbrains.bigdatatools.glue.monitoring.controllers

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.table.getSelectedData
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TableWithDetailsMonitoringController
import com.jetbrains.bigdatatools.common.ui.BdtJsonInfoDialog
import com.jetbrains.bigdatatools.common.ui.ToolbarLabelActionImpl
import com.jetbrains.bigdatatools.glue.client.GlueDataManager
import com.jetbrains.bigdatatools.glue.monitoring.models.DatabaseId
import com.jetbrains.bigdatatools.glue.monitoring.models.GlueDatabaseInfo
import com.jetbrains.bigdatatools.glue.monitoring.models.GlueResourceShareType
import com.jetbrains.bigdatatools.glue.settings.GlueConnectionData
import com.jetbrains.bigdatatools.glue.settings.GlueToolWindowSettings
import com.jetbrains.bigdatatools.glue.utils.GlueMessagesBundle
import software.amazon.awssdk.services.glue.model.Database
import javax.swing.JComponent

class GlueDatabasesController(val project: Project,
                              connectionData: GlueConnectionData) : TableWithDetailsMonitoringController<GlueDatabaseInfo, String>() {
  private val dataManager = GlueDataManager.getInstance(connectionData.innerId, project) ?: error("DataManager is not inited")

  override val detailsController = GlueTableController(project, dataManager)

  init {
    detailsSplitter.proportion = 0.3f
    init()
  }

  override fun showColumnFilter() = false

  override fun getColumnSettings() = GlueToolWindowSettings.getInstance().databaseSettings

  override fun getRenderableColumns() = GlueDatabaseInfo.renderableColumns

  override fun getDataModel() = dataManager.rootModel

  override fun indexToDetailId(row: Int): String? {
    val infoAt = dataTable.getDataAt(row) ?: return null
    return DatabaseId(infoAt.catalog, infoAt.name).toString()
  }

  override fun saveSelectedItem() = Unit

  override fun getToolbarTitle() = GlueMessagesBundle.message("block.databases")

  override fun getAdditionalActions(): List<AnAction> {
    val detailsAction = object : DumbAwareAction(GlueMessagesBundle.message("action.show.database.details"), null,
                                                 AllIcons.FileTypes.Json) {
      override fun actionPerformed(e: AnActionEvent) {
        val database = getDatabase() ?: return
        BdtJsonInfoDialog(project, database.name(), database).show()
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
    val settings = GlueToolWindowSettings.getInstance()
    val config = settings.getOrCreateConfig(dataManager.connectionId)

    val statusFilter = object : ComboBoxAction() {
      override fun isDumbAware(): Boolean = true

      override fun createPopupActionGroup(button: JComponent, context: DataContext): DefaultActionGroup {
        val actions = GlueResourceShareType.entries.map { shareType ->
          val text = shareType.title

          DumbAwareAction.create(text) {
            config.databaseResourceShareType = shareType.awsType?.name
            reloadTables()
          }
        }
        return DefaultActionGroup(actions)
      }

      override fun update(e: AnActionEvent) {
        val typeId = config.databaseResourceShareType

        val type = GlueResourceShareType.fromId(typeId)
        e.presentation.text = type.title
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    statusFilter.templatePresentation.text = GlueMessagesBundle.message("filter.database.type")
    statusFilter.templatePresentation.icon = AllIcons.General.Filter

    return listOf(ToolbarLabelActionImpl(GlueMessagesBundle.message("filter.text")), statusFilter)
  }

  private fun reloadTables() {
    dataManager.updater.invokeRefreshModel(dataManager.rootModel)
  }
}