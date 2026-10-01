package com.intellij.dataWrangler.impl.ui

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.database.datagrid.DataGrid

interface DWTableDataViewer {
  /**
   * Set a column highlighted
   *
   * @param name Name of the column
   */
  fun setColumnHighlighted(name: String, intent: ColumnIntent)
  fun dropColumnHighlights()
  fun getGrid(): DataGrid

}