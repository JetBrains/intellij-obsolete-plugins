package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.table.DataTableCreator
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableColumnModel
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableModel
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.table.MaterialJBScrollPane
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.hadoop.monitoring.data.HadoopDataManager
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.NodeLabel
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings
import java.util.EnumSet

class NodeLabels(project: Project, connectionData: HadoopConnectionData) : Disposable {

  private val panel: SimpleToolWindowPanel
  private val dataManager = HadoopDataManager.getInstance(connectionData.innerId, project) ?: error("Data Manager is not inited")

  init {
    val nodesModel = dataManager.getNodeLabelsModel()

    val nodeColumnSettings = HadoopSettings.getInstance().nodeLabelColumnSettings

    val columnModel = DataTableColumnModel(NodeLabel.renderableColumns, nodeColumnSettings)
    val tableModel = DataTableModel(nodesModel, columnModel)

    val table = DataTableCreator.create(tableModel, EnumSet.of(TableExtensionType.SPEED_SEARCH,
                                                               TableExtensionType.RENDERERS_SETTER,
                                                               TableExtensionType.COLUMNS_FITTER,
                                                               TableExtensionType.ERROR_HANDLER,
                                                               TableExtensionType.SELECTION_PRESERVER,
                                                               TableExtensionType.LOADING_INDICATOR,
                                                               TableExtensionType.SMART_RESIZER))

    Disposer.register(this, table)

    panel = SimpleToolWindowPanel(false, true).apply {
      setContent(MaterialJBScrollPane(table))
      val actionToolbar = createToolbar(columnModel)
      actionToolbar.targetComponent = this
      toolbar = actionToolbar.component
    }
  }

  fun getComponent() = panel

  private fun createToolbar(columnModel: DataTableColumnModel<NodeLabel>): ActionToolbar {
    val configStoragesColumnsAction = ColumnVisibilitySettings.createAction(columnModel.allColumns,
                                                                            HadoopSettings.getInstance().nodeLabelColumnSettings)

    return ToolbarUtils.createActionToolbar("BDTHadoopNodeLabels", DefaultActionGroup(configStoragesColumnsAction), false)
  }

  override fun dispose() = Unit
}