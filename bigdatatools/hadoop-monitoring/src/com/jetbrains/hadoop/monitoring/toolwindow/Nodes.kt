package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.SmartPopupActionGroup
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.table.DataTableCreator
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableColumnModel
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableModel
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.table.MaterialJBScrollPane
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.hadoop.monitoring.data.HadoopDataManager
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.NodeInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.NodeState
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings
import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle
import java.util.EnumSet
import javax.swing.JTable
import javax.swing.RowFilter
import javax.swing.table.TableRowSorter

class Nodes(project: Project, connectionData: HadoopConnectionData) : Disposable {

  private val panel: SimpleToolWindowPanel
  private val dataManager = HadoopDataManager.getInstance(connectionData.innerId, project) ?: error("Data Manager is not inited")

  init {
    val nodesModel = dataManager.getNodesModel()

    val nodeColumnSettings = HadoopSettings.getInstance().nodeColumnSettings

    val columnModel = DataTableColumnModel(NodeInfo.renderableColumns, nodeColumnSettings)
    val tableModel = DataTableModel(nodesModel, columnModel)

    val table = DataTableCreator.create(tableModel, EnumSet.of(TableExtensionType.SPEED_SEARCH,
                                                               TableExtensionType.RENDERERS_SETTER,
                                                               TableExtensionType.COLUMNS_FITTER,
                                                               TableExtensionType.ERROR_HANDLER,
                                                               TableExtensionType.SELECTION_PRESERVER,
                                                               TableExtensionType.LOADING_INDICATOR,
                                                               TableExtensionType.SMART_RESIZER))
    Disposer.register(this, table)

    setupTableFilter(table, tableModel, columnModel)

    panel = SimpleToolWindowPanel(false, true).apply {
      setContent(MaterialJBScrollPane(table))
      val actionToolbar = createToolbar(table, columnModel)
      actionToolbar.targetComponent = this
      toolbar = actionToolbar.component
    }
  }

  fun getComponent() = panel

  private fun createToolbar(table: DataTable<NodeInfo>, columnModel: DataTableColumnModel<NodeInfo>): ActionToolbar {

    val settings = HadoopSettings.getInstance()

    val actions = DefaultActionGroup()

    val statusFilter = SmartPopupActionGroup()
    statusFilter.templatePresentation.text = HadoopMessagesBundle.message("nodes.filter.state")
    statusFilter.templatePresentation.icon = AllIcons.General.Filter

    for (status in NodeState.entries) {
      val toggleState = object : DumbAwareToggleAction(status.text, null, IconUtils.getIconForNodeStatus(status)) {
        override fun isSelected(e: AnActionEvent) = settings.nodeStates.contains(status)
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) {
          if (state) {
            settings.nodeStates.add(status)
          }
          else {
            settings.nodeStates.remove(status)
          }
          table.rowSorter?.allRowsChanged()
          dataManager.updater.invokeRefreshModel(dataManager.getNodesModel())
        }
      }

      statusFilter.add(toggleState)
    }

    actions.add(statusFilter)

    val configStoragesColumnsAction = ColumnVisibilitySettings.createAction(columnModel.allColumns, settings.nodeColumnSettings)
    actions.add(configStoragesColumnsAction)

    return ToolbarUtils.createActionToolbar("BDTHadoopNodes", actions, false)
  }

  private fun setupTableFilter(table: JTable, tableModel: DataTableModel<NodeInfo>, columnModel: DataTableColumnModel<NodeInfo>) {
    val sorter = TableRowSorter(tableModel)

    val filter = object : RowFilter<DataTableModel<NodeInfo>, Int>() {
      private val statusColumnIndex = columnModel.getModelIndex("state")
      override fun include(entry: Entry<out DataTableModel<NodeInfo>, out Int>): Boolean {
        return HadoopSettings.getInstance().nodeStates.contains(entry.getValue(statusColumnIndex) as NodeState)
      }
    }

    sorter.rowFilter = filter
    table.rowSorter = sorter
  }

  override fun dispose() {}
}