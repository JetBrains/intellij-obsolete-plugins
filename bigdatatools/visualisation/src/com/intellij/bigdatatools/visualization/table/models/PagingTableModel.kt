package com.intellij.bigdatatools.visualization.table.models

import com.intellij.charts.dataframe.DataFrame
import com.intellij.charts.dataframe.columns.IntegerType
import com.intellij.charts.dataframe.columns.LongType
import com.intellij.charts.dataframe.columns.RealType
import com.intellij.charts.dataframe.columns.StringType
import javax.swing.table.AbstractTableModel
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class PagingTableModel(val dataFrame: DataFrame) : AbstractTableModel() {

  /** Number of records per page. */
  var pageSize = 10
    set(value) {
      if (value == field) {
        return
      }
      val oldPageSize = field
      field = value
      pageOffset = oldPageSize * pageOffset / field
      fireTableDataChanged()
    }

  /** Index of current page. */
  var pageOffset = 0
    set(value) {
      field = when {
        value < 0 -> 0
        value >= getPageCount() -> max(0, getPageCount() - 1)
        else -> value
      }
      fireTableDataChanged()
    }

  /** Possible visible rows count for current page. */
  override fun getRowCount(): Int {
    return min(dataFrame.rowsCount - pageOffset * pageSize, pageSize)
  }

  override fun getColumnCount(): Int {
    return dataFrame.columnsCount
  }

  override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
    val realRow = rowIndex + pageOffset * pageSize
    return dataFrame[columnIndex].toList()[realRow]
  }

  override fun getColumnName(columnIndex: Int): String {
    @Suppress("HardCodedStringLiteral") // Dataframe column names comes from user data.
    return dataFrame[columnIndex].name
  }

  fun getPageCount(): Int {
    return ceil(dataFrame.rowsCount / pageSize.toDouble()).toInt()
  }

  /** Real row count in dataframe. */
  fun getRealRowCount(): Int {
    return dataFrame.rowsCount
  }

  override fun getColumnClass(columnIndex: Int): Class<*> {
    val columnType = dataFrame[columnIndex].type
    return when {
      columnType.isArray() -> super.getColumnClass(columnIndex)
      columnType == StringType -> String::class.java
      columnType == IntegerType -> Int::class.java
      columnType == LongType -> Long::class.java
      columnType == RealType -> Double::class.java
      else -> super.getColumnClass(columnIndex)
    }
  }
}