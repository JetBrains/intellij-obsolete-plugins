package com.intellij.bigdatatools.notebooks.core.api.editor

import com.google.gson.JsonElement
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell

interface NoteEditorActionListener {
  fun runAll() {}
  fun clearAllOutput() {}
  fun clearCellOutput(cell: NotebookCell) {}
  fun stopAll() {}
  fun runCell(cell: NotebookCell) {}
  fun stopCell(cell: NotebookCell) {}
  fun moveCell(cell: NotebookCell, toIndex: Int) {}
  fun runAllBelow(cell: NotebookCell) {}
  fun runAllAbove(cell: NotebookCell) {}
  fun runCellGoBelow(cell: NotebookCell) {}
  fun addPlanningExecutingCellsFromLocalQueue(cells: List<NotebookCell>) {}
  fun changeCellTitleIsVisible(cell: NotebookCell) {}
  fun restartInterpreter(cell: NotebookCell) {}
  fun deleteCell(cell: NotebookCell) {}
  fun addCell(cellIndex: Int, cellText: String) {}
  fun addCell(cellIndex: Int,
              cellText: String,
              isTableHidden: Boolean,
              isEditorHidden: Boolean,
              metadata: Map<String, JsonElement>,
              scrollToCell: Boolean) {
  }

  fun cloneCell(cell: NotebookCell, index: Int) {}
  fun splitCell(cell: NotebookCell, lineOffset: Int) {}
  fun mergeWithNext(cell: NotebookCell) {}
}