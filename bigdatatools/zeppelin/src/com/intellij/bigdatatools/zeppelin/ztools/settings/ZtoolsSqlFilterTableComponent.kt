@file:Suppress("DuplicatedCode")

package com.intellij.bigdatatools.zeppelin.ztools.settings

import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper.IS_VISUAL_PADDING_COMPENSATED_ON_COMPONENT_LEVEL_KEY
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import net.miginfocom.layout.CC
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.JPanel

class ZtoolsSqlFilterTableComponent(val project: Project,
                                    initValues: List<ZtoolFilterRow>) {
  private val table = run {
    val columns = arrayOf(DatabaseColumn(), TableRegexColumn())
    val tableModel = createTableModel(columns)
    val tableView = TableView(tableModel)
    tableView.emptyText.text = ZepMessagesBundle.message("ztools.settings.sql.filter.empty")
    tableView
  }

  init {
    initValues.forEach {
      table.listTableModel.addRow(TableRow(it.database, it.tablePattern))
    }
  }

  fun getComponent() = MigPanel().apply {
    add(JLabel(ZepMessagesBundle.message("ztools.settings.sql.filter.label")), CC().gapTop("3").spanX().wrap())
    add(createTableDecorator(table), CC().growX().growY().pushY().spanX().wrap())

    putClientProperty(IS_VISUAL_PADDING_COMPENSATED_ON_COMPONENT_LEVEL_KEY, true)

    minimumSize = Dimension(400, minimumSize.height + (table.rowCount + 1) * table.rowHeight)
  }

  fun getResult() = table.listTableModel.items
    .map { ZtoolFilterRow(it.key, it.value) }
    .filter { it.tablePattern.isNotBlank() || it.database.isNotBlank() }

  private fun createTableModel(columns: Array<ColumnInfo<TableRow, Any?>>) = ListTableModel(columns, mutableListOf<TableRow>(), -1)

  private class DatabaseColumn : ColumnInfo<TableRow, Any?>(ZepMessagesBundle.message("ztools.settings.sql.filter.column.database")) {
    override fun valueOf(item: TableRow) = item.key
    override fun isCellEditable(item: TableRow?): Boolean = true

    override fun setValue(item: TableRow, value: Any?) {
      item.key = value?.toString() ?: ""
    }
  }

  private class TableRegexColumn : ColumnInfo<TableRow, Any?>(ZepMessagesBundle.message("ztools.settings.sql.filter.column.table.regex")) {
    override fun valueOf(item: TableRow) = item.value
    override fun isCellEditable(item: TableRow?): Boolean = true

    override fun setValue(item: TableRow, value: Any?) {
      item.value = value?.toString() ?: ""
    }
  }

  private fun createTableDecorator(tableView: TableView<TableRow>): JPanel {
    val model = tableView.listTableModel

    return ToolbarDecorator.createDecorator(tableView)
      .disableUpAction()
      .disableDownAction()
      .setAddAction {
        model.addRow(TableRow("", ""))
      }.setRemoveAction {
        model.removeRow(tableView.selectedRow)
      }.createPanel()
  }

  private data class TableRow(var key: String, var value: String)
}