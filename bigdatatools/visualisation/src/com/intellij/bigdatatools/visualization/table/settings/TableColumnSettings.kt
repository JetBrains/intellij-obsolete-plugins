package com.intellij.bigdatatools.visualization.table.settings

data class TableColumnSettings(
  val name: String,
  val visible: Boolean,
  val sort: TableColumnSortingSettings?,
  // In zeppelin we have filters
)