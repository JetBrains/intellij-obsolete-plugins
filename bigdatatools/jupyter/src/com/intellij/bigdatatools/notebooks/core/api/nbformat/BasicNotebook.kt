package com.intellij.bigdatatools.notebooks.core.api.nbformat

import com.google.gson.JsonObject
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.NotebookMetadataAware

interface BasicNotebook : NotebookMetadataAware {
  val delimiter: String
  val schema: NotebookSchema
  val stemCell: NotebookCell?
  val cells: List<NotebookCell>
  val json: JsonObject

  val hasRunningCells: Boolean
    get() = cells.any { it.isLaunched }

  fun addCell(cell: NotebookCell, index: Int)
  fun addNewCell(source: String, index: Int)
  fun removeCell(index: Int)
  fun removeCell(cell: NotebookCell)
  fun updateCell(newCell: NotebookCell, index: Int)
  fun replace(newNote: BasicNotebook)
  fun moveCell(cell: NotebookCell, toIndex: Int)

  /**
   * Create a new cell, associated with this notebook
   */
  fun buildCell(text: String, cellType: NotebookCellType): NotebookCell

  /**
   * Get content of the notebook as a source code with cells.
   */
  fun asSource(): String

  /**
   * Get content of the notebook as a JSON that is valid ipynb.
   */
  fun asJson(): CharSequence

  /**
   * Change notebook model, all changes with notebook must be performed by this function
   */
  fun performModification(body: () -> Unit)
  fun startModificationSection()
  fun endModificationSection()

  fun addNotebookChangeListener(notebookChangeListener: NotebookChangeListener)
  fun removeNotebookChangeListener(notebookChangeListener: NotebookChangeListener)
  fun updateTimeStamp(): Long
  fun getCellByOffset(offset: Int): NotebookCell?
  fun createCellFromText(textMarker: String): NotebookCell
}