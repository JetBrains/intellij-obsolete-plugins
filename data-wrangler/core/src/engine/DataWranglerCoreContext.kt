package com.intellij.dataWrangler.core.engine

import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.database.datagrid.DataGrid
import com.intellij.database.datagrid.GridColumn
import com.intellij.database.datagrid.GridDataHookUp
import com.intellij.database.datagrid.GridMutator.RowsMutator
import com.intellij.database.datagrid.GridRequestSource
import com.intellij.database.datagrid.GridRow
import com.intellij.database.datagrid.GridUtil
import com.intellij.database.datagrid.ModelIndex
import com.intellij.database.datagrid.ModelIndexSet
import com.intellij.database.extractors.BaseObjectFormatter
import com.intellij.database.extractors.DatabaseObjectFormatterConfig
import com.intellij.database.extractors.ObjectFormatterConfig
import com.intellij.database.extractors.ObjectFormatterMode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

// TODO: rewrite
internal class DataWranglerCoreContext(dataGrid: DataGrid) : DataWranglerContext {
  private val gridDataHookUp = dataGrid.dataHookup

  fun getText(rowId: Int, colId: Int) = getText(rowId, colId, gridDataHookUp)

  override fun getProject(): Project = gridDataHookUp.project

  override fun getColumnNames() = gridDataHookUp.dataModel.columns.map { it.name }
  override fun getTableName(): String = ""
  override fun getFile(): VirtualFile? = null

  override fun dispose() = Unit

  fun getGridDataHookUp() = gridDataHookUp

  companion object {

    private val LOG = Logger.getInstance(this::class.java)

    /**
     * Inspired by [GridUtil.getText]. Use this method if you have [DataGrid] instance
     */
    fun getText(
      rowId: Int, colId: Int, gridDataHookUp: GridDataHookUp<GridRow, GridColumn>,
    ): String {
      val rowIdx = ModelIndex.forRow(gridDataHookUp.mutationModel, rowId)
      val columnIdx = ModelIndex.forColumn(gridDataHookUp.mutationModel, colId)
      val model = gridDataHookUp.getDataModel()
      val row = model.getRow(rowIdx) ?: return GridUtil.NULL_TEXT
      val column = model.getColumn(columnIdx) ?: return GridUtil.NULL_TEXT

      // in the original code [GridUtil.createFormatterConfig] is used
      return getText(row, column, DatabaseObjectFormatterConfig.get(ObjectFormatterMode.DEFAULT))
    }

    private fun getText(row: GridRow, column: GridColumn, config: ObjectFormatterConfig): String {
      val value = BaseObjectFormatter().objectToString(column.getValue(row), column, config)
      return value ?: GridUtil.NULL_TEXT
    }

    internal fun deleteRows(ids: List<Int>, gridDataHookUp: GridDataHookUp<GridRow, GridColumn>) {
      val mutator = gridDataHookUp.mutator as? RowsMutator
      if (mutator == null) {
        LOG.error("Unsupported instance of mutator in GridDataHookUp")
        return
      }

      mutator.deleteRows(GridRequestSource(null), ModelIndexSet.forRows(gridDataHookUp.mutationModel, *ids.toIntArray()))
    }
  }
}