package com.intellij.python.huggingFace.cacheManager.ui.core

import com.intellij.util.ui.JBUI
import javax.swing.JTable
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

enum class ColumnWidthFittingStrategy {
  PERCENTILE_BASED,
  MAX_WIDTH_BASED
}

object MaterialTableUtils {
  // Column width detection for numeric column is not percentile 75 based; it will always maxWidth.
  private val numericClasses = hashSetOf(Int::class.java, Double::class.java, Long::class.java, Float::class.java)

  private fun getColumnHeaderWidth(table: JTable, column: Int): Int {

    if (table.tableHeader == null || table.columnModel.columnCount <= column) {
      return 0
    }

    val tableColumn = table.columnModel.getColumn(column)
    var renderer = tableColumn.headerRenderer

    if (renderer == null) {
      renderer = table.tableHeader.defaultRenderer
    }

    val value = tableColumn.headerValue
    val c = renderer!!.getTableCellRendererComponent(table, value, false, false, -1, column)
    return c.preferredSize.width
  }

  private fun getColumnWidth(table: JTable, columnIndex: Int, strategy: ColumnWidthFittingStrategy): Int {
    var headerWidth = max(JBUI.scale(35), getColumnHeaderWidth(table, columnIndex))
    headerWidth = min(headerWidth, 350)

    if (table.rowCount == 0) {
      return headerWidth + JBUI.scale(10)
    }

    var averageWidth = headerWidth

    if (strategy == ColumnWidthFittingStrategy.PERCENTILE_BASED && !numericClasses.contains(table.getColumnClass(columnIndex))) {
      //  We want to take 50 samples
      var step = table.rowCount / 50
      if (step == 0) {
        step = 1
      }

      val widths = Array(ceil(table.rowCount / step.toDouble()).toInt()) { 0 }

      for ((i, row) in (0 until table.rowCount step step).withIndex()) {
        val renderer = table.getCellRenderer(row, columnIndex)
        val comp = table.prepareRenderer(renderer, row, columnIndex)
        widths[i] = comp.preferredSize.width
      }

      widths.sort()

      // percentile 75
      averageWidth = widths[(widths.size * 0.75).toInt()]

      if (averageWidth < 350) {
        averageWidth = min(350, widths.maxOrNull() ?: JBUI.scale(35))
      }
    }
    else {
      for (i in 0 until table.rowCount) {
        val renderer = table.getCellRenderer(i, columnIndex)
        val comp = table.prepareRenderer(renderer, i, columnIndex)
        averageWidth = max(averageWidth, comp.preferredSize.width)
      }
    }

    return max(headerWidth, averageWidth + JBUI.scale(10))
  }

  private fun fitColumnWidth(columnIndex: Int,
                             table: JTable,
                             strategy: ColumnWidthFittingStrategy = ColumnWidthFittingStrategy.MAX_WIDTH_BASED,
                             maxWidth: Int = Int.MAX_VALUE) {
    var width = getColumnWidth(table, columnIndex, strategy)

    if (maxWidth in 1 until width) {
      width = maxWidth
    }

    table.columnModel.getColumn(columnIndex).preferredWidth = width
  }

  /**
   * Fits the table columns widths by guideline https://jetbrains.github.io/ui/controls/table/
   */
  fun fitColumnsWidth(table: JTable,
                      strategy: ColumnWidthFittingStrategy = ColumnWidthFittingStrategy.MAX_WIDTH_BASED,
                      maxWidth: Int = Int.MAX_VALUE) {
    for (columnIndex in 0 until table.columnCount) {
      fitColumnWidth(columnIndex, table, strategy, maxWidth)
    }
  }

}