package com.intellij.bigdatatools.zeppelin.interpreter

import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterPropertyType
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.containers.ContainerUtil
import java.awt.Component
import java.text.DecimalFormat
import java.util.EventObject
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.event.CellEditorListener
import javax.swing.event.ChangeEvent
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

/** Special table cell renderer/editor for values column of properties table.
 *  Every row could be of its own type.
 */
class InterpreterPropertiesRenderer : TableCellRenderer, TableCellEditor {

  companion object {
    val instance = InterpreterPropertiesRenderer()
  }

  class NumberField : JBTextField()

  var format = DecimalFormat("#.###############")

  private val listeners = ContainerUtil.createLockFreeCopyOnWriteList<CellEditorListener>()

  private val components = mutableMapOf<InterpreterPropertyType, JComponent>(Pair(InterpreterPropertyType.textarea, JBTextField()),
                                                                             Pair(InterpreterPropertyType.text, JBTextField()),
                                                                             Pair(InterpreterPropertyType.boolean, JBCheckBox()),
                                                                             Pair(InterpreterPropertyType.string, JBTextField()),
                                                                             Pair(InterpreterPropertyType.number, NumberField()),
                                                                             Pair(InterpreterPropertyType.url, JBTextField()),
                                                                             Pair(InterpreterPropertyType.password, JBPasswordField()),
                                                                             Pair(InterpreterPropertyType.checkbox, JBCheckBox()))

  private var currentEditor: JComponent? = null

  private fun getComponent(table: JTable, value: Any?, row: Int, isSelected: Boolean): JComponent {
    val columnType = table.getValueAt(row, 2) as InterpreterPropertyType
    val component = components[columnType]
    when (columnType) {
      InterpreterPropertyType.checkbox, InterpreterPropertyType.boolean -> (component as JBCheckBox).isSelected = value as? Boolean ?: false
      InterpreterPropertyType.password -> (component as JBPasswordField).text = value as? String ?: ""
      InterpreterPropertyType.number -> (component as JBTextField).text = if (value is Number) format.format(value) else value.toString()
      else -> (component as JBTextField).text = value.toString()
    }

    if (isSelected) {
      component.foreground = table.selectionForeground
      component.background = table.selectionBackground
    }
    else {
      component.foreground = table.foreground
      component.background = table.background
    }

    return component
  }

  // region TableCellRenderer
  override fun getTableCellRendererComponent(table: JTable,
                                             value: Any?,
                                             isSelected: Boolean,
                                             hasFocus: Boolean,
                                             row: Int,
                                             column: Int): Component {
    return getComponent(table, value, row, isSelected)
  }
  //endregion TableCellRenderer

  //region TableCellEditor
  override fun getCellEditorValue(): Any? {
    return when (val localEditor = currentEditor) {
      null -> null
      is NumberField -> localEditor.text.toIntOrNull() ?: localEditor.text.toDoubleOrNull() ?: localEditor.text
      is JBPasswordField -> String(localEditor.password)
      is JBTextField -> localEditor.text
      is JBCheckBox -> localEditor.isSelected
      else -> null
    }
  }

  override fun isCellEditable(anEvent: EventObject?) = true

  override fun shouldSelectCell(anEvent: EventObject?) = true

  override fun stopCellEditing(): Boolean {
    val e = ChangeEvent(this)
    listeners.forEach { it.editingStopped(e) }
    currentEditor = null
    return true
  }

  override fun cancelCellEditing() {
    val e = ChangeEvent(this)
    listeners.forEach { it.editingCanceled(e) }
    currentEditor = null
  }

  override fun addCellEditorListener(l: CellEditorListener?) {
    listeners.add(l)
  }

  override fun removeCellEditorListener(l: CellEditorListener?) {
    listeners.remove(l)
  }

  override fun getTableCellEditorComponent(table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
    val component = getComponent(table, value, row, isSelected)
    currentEditor = component
    return component
  }
  //endregion TableCellEditor
}