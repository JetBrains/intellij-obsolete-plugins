package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ScrollPaneFactory
import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.table.DataTableCreator
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableColumnModel
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableModel
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.ui.ToolbarVerticalLabelAction
import com.jetbrains.bigdatatools.common.ui.setCenterComponent
import com.jetbrains.bigdatatools.common.ui.setLineStartComponent
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ContainerInfo
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings
import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle
import java.awt.BorderLayout
import java.util.EnumSet
import javax.swing.JComponent
import javax.swing.JPanel

class Containers : Disposable {

  private val panel: JPanel

  private val table: DataTable<ContainerInfo>

  init {
    val settings = HadoopSettings.getInstance()

    val containerInfoColumnSettings = settings.containerInfoColumnSettings

    val columnModel = DataTableColumnModel(ContainerInfo.renderableColumns, containerInfoColumnSettings)
    val tableModel = DataTableModel(null, columnModel)

    table = DataTableCreator.create(tableModel, EnumSet.of(TableExtensionType.SPEED_SEARCH,
                                                           TableExtensionType.RENDERERS_SETTER,
                                                           TableExtensionType.COLUMNS_FITTER,
                                                           TableExtensionType.ERROR_HANDLER,
                                                           TableExtensionType.SELECTION_PRESERVER,
                                                           TableExtensionType.SMART_RESIZER))
    Disposer.register(this, table)

    panel = JPanel(BorderLayout()).apply {
      val actionToolbar = createToolbar(columnModel)
      actionToolbar.targetComponent = this
      setLineStartComponent(actionToolbar.component)
      setCenterComponent(ScrollPaneFactory.createScrollPane(table, true))
    }
  }

  fun getComponent(): JComponent {
    return panel
  }

  private fun createToolbar(columnModel: DataTableColumnModel<ContainerInfo>): ActionToolbar {
    val settings = HadoopSettings.getInstance()
    val containerColumnVisibilityAction = ColumnVisibilitySettings.createAction(columnModel.allColumns,
                                                                                settings.containerInfoColumnSettings)

    val actions = DefaultActionGroup(
      ToolbarVerticalLabelAction.create(HadoopMessagesBundle.message("application.attempt.containerInfo.header")),
      Separator(),
      containerColumnVisibilityAction)

    return ToolbarUtils.createActionToolbar("BDTHadoopContainers", actions, horizontal = false)
  }

  fun setDataModel(data: ObjectDataModel<ContainerInfo>) {
    table.tableModel.setDataModel(data)
  }

  override fun dispose() {}
}