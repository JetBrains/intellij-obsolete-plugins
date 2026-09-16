package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.table.DataTableCreator
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableLoadingDecorator
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableColumnModel
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableModel
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.ui.ToolbarVerticalLabelAction
import com.jetbrains.bigdatatools.common.ui.setCenterComponent
import com.jetbrains.bigdatatools.common.ui.setLineStartComponent
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppAttemptInfo
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings
import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle
import java.awt.BorderLayout
import java.util.EnumSet
import javax.swing.JComponent
import javax.swing.JPanel

class ApplicationAttempts : Disposable {

  private val panel: JPanel

  val table: DataTable<AppAttemptInfo>

  init {
    val settings = HadoopSettings.getInstance()

    val appAttemptColumnSettings = settings.appAttemptColumnSettings

    val columnModel = DataTableColumnModel(AppAttemptInfo.renderableColumns, appAttemptColumnSettings)
    val tableModel = DataTableModel(null, columnModel)

    table = DataTableCreator.create(tableModel, EnumSet.of(TableExtensionType.SPEED_SEARCH,
                                                           TableExtensionType.RENDERERS_SETTER,
                                                           TableExtensionType.COLUMNS_FITTER,
                                                           TableExtensionType.ERROR_HANDLER,
                                                           TableExtensionType.SELECTION_PRESERVER,
                                                           TableExtensionType.SMART_RESIZER))
    Disposer.register(this, table)

    panel = JPanel(BorderLayout()).apply {
      val toolbar = createToolbar(columnModel)
      toolbar.targetComponent = this
      setLineStartComponent(toolbar.component)
      setCenterComponent(JBScrollPane(table))
    }
  }

  fun getComponent(): JComponent = panel

  private fun createToolbar(columnModel: DataTableColumnModel<AppAttemptInfo>): ActionToolbar {
    val attemptsColumnVisibilityAction = ColumnVisibilitySettings.createAction(columnModel.allColumns,
                                                                               HadoopSettings.getInstance().appAttemptColumnSettings)

    val actions = DefaultActionGroup(
      ToolbarVerticalLabelAction.create(HadoopMessagesBundle.message("application.attempts.header")),
      Separator(),
      attemptsColumnVisibilityAction)

    return ToolbarUtils.createActionToolbar("BDTHadoopApplicationAttempts", actions, horizontal = false)
  }

  fun setDataModel(data: ObjectDataModel<AppAttemptInfo>) {
    table.tableModel.setDataModel(data)
    TableLoadingDecorator.installOn(table)
  }

  override fun dispose() {}
}