package com.intellij.python.huggingFace.cacheManager.ui.core

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Component
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.table.TableCellRenderer

internal class HfCacheSimpleHeaderRenderer : JLabel(), TableCellRenderer {
  init {
    horizontalTextPosition = LEADING
    foreground = JBColor.DARK_GRAY
    font = UIUtil.getLabelFont()
  }

  private fun getSortingIcon(column: Int, sortKeys: List<RowSorter.SortKey>): Icon? {
    val sortKey = sortKeys.firstOrNull { it.column == column } ?: return null
    return when (sortKey.sortOrder) {
      SortOrder.ASCENDING -> AllIcons.General.ChevronDown
      SortOrder.DESCENDING -> AllIcons.General.ChevronUp
      else -> null
    }
  }

  override fun getTableCellRendererComponent(table: JTable,
                                             value: Any?,
                                             isSelected: Boolean,
                                             hasFocus: Boolean,
                                             row: Int,
                                             column: Int): Component {
    icon = if (table.columnCount <= column) null else getSortingIcon(table.convertColumnIndexToModel(column), table.rowSorter.sortKeys)
    text = " ${value.toString()} "
    return this
  }
}