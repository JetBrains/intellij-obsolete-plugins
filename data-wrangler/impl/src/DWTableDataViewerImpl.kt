package com.intellij.dataWrangler.impl

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.impl.ui.DWTableDataViewer
import com.intellij.database.datagrid.DataGrid
import com.intellij.database.datagrid.GridRow
import com.intellij.database.datagrid.ModelIndex
import com.intellij.database.datagrid.ModelIndexSet
import com.intellij.database.datagrid.color.GridColorModelImpl
import com.intellij.database.datagrid.color.TableHeatmapColorLayer
import com.intellij.database.diff.TableDiffColors
import com.intellij.database.run.ui.grid.CellAttributesKey
import com.intellij.database.run.ui.grid.GridMarkupModel
import com.intellij.database.run.ui.table.TableResultView

class DWTableDataViewerImpl(private val grid: DataGrid) : DWTableDataViewer {
  private val tableView = grid.resultView as TableResultView
  private var myHighlightings: List<GridMarkupModel.Highlighting> = emptyList()

  private val colorModel
    get() = grid.colorModel as? GridColorModelImpl
  private val tableHeatmapLayer
    get() = colorModel?.getLayer(TableHeatmapColorLayer::class.java) as? TableHeatmapColorLayer

  private var currentColorMode: TableHeatmapColorLayer.ColoringMode? = null

  override fun getGrid(): DataGrid = grid
  override fun setColumnHighlighted(name: String, intent: ColumnIntent) {

    myHighlightings.forEach { highlight ->
      grid.markupModel.removeHighlighting(highlight)
    }
    myHighlightings = emptyList()

    // save color mode
    if (currentColorMode == null) {
      currentColorMode = tableHeatmapLayer?.coloringMode
    }
    tableHeatmapLayer?.coloringMode = TableHeatmapColorLayer.ColoringMode.OFF

    val column = grid.dataHookup.dataModel.columns.find { it.name == name } ?: return
    val rows: ModelIndexSet<GridRow> = grid.visibleRows
    val columnIdx = ModelIndex.forColumn(grid, column.columnNumber)
    val columns = ModelIndexSet.forColumns(grid.dataHookup.dataModel, columnIdx)
    val highlightColor = getHighlightColorForIntent(intent)
    val attributesKey = CellAttributesKey(highlightColor, false)
    val cellHighlighting = grid.markupModel.highlightCells(rows, columns, attributesKey, 1)
    val headerHighlighting = grid.markupModel.highlightColumnHeaders(columns, attributesKey, 0)

    myHighlightings = listOf(cellHighlighting, headerHighlighting)

    // Navigate to the column
    val rect = tableView.getCellRect(0, columnIdx.toView(grid).asInteger(), false)
    tableView.scrollRectToVisible(rect)

    grid.mainResultViewComponent.repaint()
  }

  override fun dropColumnHighlights() {
    myHighlightings.forEach { highlight ->
      grid.markupModel.removeHighlighting(highlight)
    }
    myHighlightings = emptyList()

    // restore color mode
    currentColorMode?.let {
      tableHeatmapLayer?.coloringMode = it
    }
    currentColorMode = null
  }

  private fun getHighlightColorForIntent(intent: ColumnIntent) = when (intent) {
    ColumnIntent.REMOVE -> TableDiffColors.TDIFF_DELETED
    ColumnIntent.ADD -> TableDiffColors.TDIFF_INSERTED
    ColumnIntent.CHANGE -> TableDiffColors.TDIFF_MODIFIED
    ColumnIntent.REFERENCE -> TableDiffColors.TDIFF_MODIFIED
  }

}
