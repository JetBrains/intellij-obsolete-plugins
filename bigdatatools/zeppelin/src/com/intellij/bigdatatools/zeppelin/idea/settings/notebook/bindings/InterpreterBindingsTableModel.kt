package com.intellij.bigdatatools.zeppelin.idea.settings.notebook.bindings

import com.intellij.bigdatatools.zeppelin.models.interpreter.Interpreter
import com.intellij.icons.AllIcons
import javax.swing.Icon
import javax.swing.table.AbstractTableModel

class InterpreterBindingsTableModel(private val bindings: MutableList<Interpreter>) : AbstractTableModel() {
  private val ourColumnNames = arrayOf("Restart", "Bind", "Name")
  private val ourColumnClasses = arrayOf(Icon::class.java, Boolean::class.java, String::class.java)

  init {
    setSelectedAllForIfNone()
  }

  private fun setSelectedAllForIfNone() {
    if (bindings.any { it.selected }) return
    val selectedBindings = bindings.map { it.copy(selected = true) }
    bindings.clear()
    bindings.addAll(selectedBindings)
  }

  override fun getColumnName(column: Int): String = ourColumnNames[column]

  override fun getColumnClass(column: Int): Class<*> = ourColumnClasses[column]

  override fun getColumnCount(): Int = 3

  override fun getRowCount(): Int = bindings.size

  override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex == 1

  override fun getValueAt(row: Int, column: Int): Any {
    val interpreter = bindings[row]
    return when (column) {
      0 -> AllIcons.Actions.Restart
      1 -> interpreter.selected
      2 -> {
        var result = interpreter.nameWithSubs
        if (row == 0) {
          result = "(default) $result"
        }
        result
      }
      else -> throw IllegalArgumentException()
    }
  }

  override fun setValueAt(value: Any?, row: Int, column: Int) {
    if (column == 1) {
      val selectedBinding = bindings[row]
      bindings[row] = selectedBinding.copy(selected = !selectedBinding.selected)
    }
  }
}