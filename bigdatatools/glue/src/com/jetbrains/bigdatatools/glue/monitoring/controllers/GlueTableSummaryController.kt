package com.jetbrains.bigdatatools.glue.monitoring.controllers

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsGroupModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractGroupFieldsModelsController
import com.jetbrains.bigdatatools.common.ui.BdtJsonInfoDialog
import com.jetbrains.bigdatatools.glue.client.GlueDataManager
import com.jetbrains.bigdatatools.glue.monitoring.models.TableId
import com.jetbrains.bigdatatools.glue.settings.GlueToolWindowSettings
import com.jetbrains.bigdatatools.glue.utils.GlueMessagesBundle
import software.amazon.awssdk.services.glue.model.Table

class GlueTableSummaryController(project: Project, override val dataManager: GlueDataManager) :
  AbstractGroupFieldsModelsController<String>(project, dataManager.client.connData.innerId) {

  override val toolWindowSettings = GlueToolWindowSettings.getInstance()

  init {
    init()
  }

  override fun getFieldsGroupModel(id: String): FieldsGroupModel<Table> {
    val tableId = id.let { TableId.fromString(it) }
    return dataManager.getTableSummary(tableId)
  }

  override fun createActions(): List<AnAction> {
    val showClusterDetailsAction = object : DumbAwareAction(GlueMessagesBundle.message("action.show.table.details"), null,
                                                            AllIcons.FileTypes.Json) {
      override fun actionPerformed(e: AnActionEvent) {
        val tableId = id?.let { TableId.fromString(it) } ?: return
        val table = dataManager.getTable(tableId.catalog, tableId.database, tableId.table) ?: return

        BdtJsonInfoDialog(project, table.name(), table).show()
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = id != null
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    return super.createActions() + listOf(showClusterDetailsAction)
  }
}