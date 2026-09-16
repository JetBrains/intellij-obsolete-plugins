package com.intellij.bigdatatools.zeppelin.dependency.ui

import java.awt.Component
import java.awt.Graphics
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

/** Special cell renderer which renders only icon and could be supplied with iconProvider*/
internal class IconCellRenderer : DefaultTableCellRenderer() {
  init {
    border = BorderFactory.createEmptyBorder()
  }

  var iconProvider: IconProvider? = null

  override fun getTableCellRendererComponent(table: JTable, value: Any, isSelected: Boolean,
                                             hasFocus: Boolean, row: Int, column: Int): Component {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    icon = iconProvider?.getIcon(row, column) ?: value as Icon
    return this
  }

  override fun setText(text: String?) {}

  override fun paintComponent(g: Graphics) {
    g.color = background
    g.fillRect(0, 0, width, height)
    icon.paintIcon(this, g, width / 2 - icon.iconWidth / 2, height / 2 - icon.iconHeight / 2)
  }
}