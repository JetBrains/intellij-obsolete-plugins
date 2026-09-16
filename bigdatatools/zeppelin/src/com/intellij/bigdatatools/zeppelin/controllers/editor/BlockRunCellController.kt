package com.intellij.bigdatatools.zeppelin.controllers.editor

import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellAdded
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellRemoved
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.editor.RangeMarker

class BlockRunCellController(val zeppelinEditor: ZeppelinEditor) : Disposable {
  private val editor = zeppelinEditor.editor
  private val note = zeppelinEditor.note
  private var guardedMarkers = mutableMapOf<NotebookCell, RangeMarker>()

  private val changeListener = object : NotebookChangeListener {
    override fun onEvent(notebookEvent: NotebookEvent) {
      when {
        notebookEvent is CellAdded -> refreshGuardMarkers()
        notebookEvent is CellRemoved -> refreshGuardMarkers()
        notebookEvent is CellChanged && notebookEvent.changedFields.contains(NotebookSchema.cellStatus) ->
          invokeAndWaitIfNeeded {
            updateCellBlock(notebookEvent.cell)
          }
      }
    }
  }

  init {
    note.addNotebookChangeListener(changeListener)
    refreshGuardMarkers()
  }

  override fun dispose() {
    note.removeNotebookChangeListener(changeListener)
  }


  private fun refreshGuardMarkers() = invokeAndWaitIfNeeded {
    note.cells.forEach { updateCellBlock(it) }
  }

  private fun updateCellBlock(cell: NotebookCell) {
    cell as ZeppelinCell
    if (cell.status == CellStatus.RUNNING ||
        cell.status == CellStatus.PENDING) {
      addMarker(cell)
    }
    else {
      removeMarker(cell)
    }
  }

  private fun removeMarker(cell: NotebookCell) {
    val marker = guardedMarkers[cell] ?: return

    editor.document.removeGuardedBlock(marker)
    guardedMarkers.remove(cell)
  }

  private fun addMarker(cell: NotebookCell) {
    removeMarker(cell)

    val cellRange = cell.textRange
    val newCellGuardMarker = editor.document.createGuardedBlock(cellRange.startOffset, cellRange.endOffset)
    newCellGuardMarker.putUserData(NotebookEditorUtils.READONLY_MARKER_REASON,
                                   ZepMessagesBundle.message("notification.executing.paragraph.no.edit"))
    newCellGuardMarker.isGreedyToLeft = true
    newCellGuardMarker.isGreedyToRight = true
    guardedMarkers[cell] = newCellGuardMarker
  }
}