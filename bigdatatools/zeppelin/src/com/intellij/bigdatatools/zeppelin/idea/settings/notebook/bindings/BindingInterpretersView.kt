package com.intellij.bigdatatools.zeppelin.idea.settings.notebook.bindings

import com.intellij.bigdatatools.zeppelin.components.containers.controller.ZeppelinNoteController
import com.intellij.bigdatatools.zeppelin.interpreter.BaseSettingsDialog
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.ui.BooleanTableCellEditor
import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.ui.ClickListener
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.scale.JBUIScale
import com.intellij.ui.table.JBTable
import java.awt.event.MouseEvent
import java.util.Collections
import javax.swing.JPanel
import javax.swing.ListSelectionModel

class BindingInterpretersView(private val controller: ZeppelinNoteController) : BaseSettingsDialog {
  val bindings = controller.bindings.toMutableList()
  private val bindingsTableModel = InterpreterBindingsTableModel(bindings)
  private val bindingsTable = JBTable(bindingsTableModel)

  init {
    bindingsTable.emptyText.text = ZepMessagesBundle.message("bindings.table.empty")
    customizeTableUI()
    bindingsTableModel.fireTableDataChanged()
  }

  /** Returns width of specified column header for table. */
  private fun getHeaderColumnWidth(table: JBTable, col: Int): Int {
    val column = table.columnModel.getColumn(col)

    var headerRenderer = column.headerRenderer
    if (headerRenderer == null) {
      headerRenderer = table.tableHeader.defaultRenderer
    }
    val headerValue = column.headerValue
    val headerComp = headerRenderer.getTableCellRendererComponent(table, headerValue, false, false, 0, col)
    return headerComp.preferredSize.width + JBUIScale.scale(10)
  }

  private fun customizeTableUI() {
    bindingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

    // Column "Restart"
    val typeColumn = bindingsTable.columnModel.getColumn(0)
    var width = getHeaderColumnWidth(bindingsTable, 0)
    typeColumn.preferredWidth = width
    typeColumn.maxWidth = width

    // Column "Bound"
    val todoCaseSensitiveColumn = bindingsTable.columnModel.getColumn(1)
    width = getHeaderColumnWidth(bindingsTable, 1)
    todoCaseSensitiveColumn.preferredWidth = width
    todoCaseSensitiveColumn.maxWidth = width
    todoCaseSensitiveColumn.cellRenderer = BooleanTableCellRenderer()
    todoCaseSensitiveColumn.cellEditor = BooleanTableCellEditor()

    object : ClickListener() {
      override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
        if (bindingsTable.selectedColumn != 0 || clickCount != 1) return true
        val interpreter = bindings[bindingsTable.selectedRow]
        controller.restartInterpreterWithConfirmation(interpreter)
        return true
      }
    }.installOn(bindingsTable)
  }

  fun getComponent() = panel {
    row { label(ZepMessagesBundle.message("bind.interpreter.description")) }
    row { cell(getPanel()).align(Align.FILL).resizableColumn() }.resizableRow()
  }

  private fun getPanel(): JPanel = ToolbarDecorator.createDecorator(bindingsTable)
    .setMoveUpAction {
      val curIndex = bindingsTable.selectedRow
      if (curIndex == 0) return@setMoveUpAction
      Collections.swap(bindings, curIndex, curIndex - 1)
      bindingsTableModel.fireTableRowsUpdated(curIndex - 1, curIndex)
      bindingsTable.setRowSelectionInterval(curIndex - 1, curIndex - 1)
    }
    .setMoveDownAction {
      val curIndex = bindingsTable.selectedRow
      if (bindingsTable.selectedRow == bindings.lastIndex) return@setMoveDownAction
      Collections.swap(bindings, curIndex, curIndex + 1)
      bindingsTableModel.fireTableRowsUpdated(curIndex, curIndex + 1)
      bindingsTable.setRowSelectionInterval(curIndex + 1, curIndex + 1)
    }.createPanel()

  override fun isModified(): Boolean {
    return controller.bindings != bindings
  }

  override fun apply(): Boolean {
    controller.saveNewInterpreterSettings(bindings)
    return true
  }

  companion object {
    private const val INTERPRETER_BINDINGS_BOUNDS = "zeppelin.notebook.bindings.bounds"

    fun showDialog(controller: ZeppelinNoteController): Boolean {
      if (!controller.checkConnectionWithNotification()) {
        return false
      }

      val bindingInterpretersView = BindingInterpretersView(controller)

      val dialogBuilder = DialogBuilder().apply {
        title(ZepMessagesBundle.message("bindings.title"))
        addOkAction()
        addCancelAction()
        setDimensionServiceKey(INTERPRETER_BINDINGS_BOUNDS)
        setCenterPanel(bindingInterpretersView.getComponent())
      }

      if (dialogBuilder.showAndGet()) {
        val newBindings = bindingInterpretersView.bindings
        controller.saveNewInterpreterSettings(newBindings)
      }

      return true
    }
  }
}