package com.intellij.bigdatatools.notebooks.core.impl.controllers

import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellAdded
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellRemoved
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.asTextRange
import com.intellij.openapi.editor.ex.FoldingListener
import com.intellij.openapi.editor.ex.FoldingModelEx
import com.intellij.openapi.editor.impl.FoldingModelImpl
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange

class NoteCellsDelimiterController(private val editor: Editor,
                                   val note: BasicNotebook) : NotebookChangeListener, FoldingListener, Disposable {
  private val folds = HashMap<NotebookCell, FoldRegion>()
  private val guardedMarkers = mutableMapOf<NotebookCell, RangeMarker>()
  private var isRequiredFullUpdate = false

  init {
    editor.putUserData(NOTE_CELL_CONTROLLER_KEY, this)
    note.cells.forEach { cell -> addOrUpdateFolding(cell) }
    note.cells.forEach { cell -> addOrUpdateMarkerGuardBlock(cell) }
    note.addNotebookChangeListener(this)
    (editor.foldingModel as? FoldingModelEx)?.addListener(this, this)
  }

  fun <T> withCellUnlock(cell: NotebookCell, body: () -> T): T = try {
    removeGuardBlock(cell)
    body()
  }
  finally {
    if (cell.indexInNote != -1)
      addGuardBlock(cell)
  }

  override fun dispose() = note.removeNotebookChangeListener(this)

  override fun onEvent(notebookEvent: NotebookEvent) {
    when (notebookEvent) {
      is CellAdded -> {
        isRequiredFullUpdate = true
      }
      is CellRemoved -> {
        isRequiredFullUpdate = true
        removeFolding(notebookEvent.cell)
        removeGuardBlock(notebookEvent.cell)
      }
      is CellChanged ->
        if (!isRequiredFullUpdate) {
          val isChangeRequires = notebookEvent.changedFields.any {
            it == "source"
          }
          if (isChangeRequires) {
            isRequiredFullUpdate = true
          }
        }
    }
    if (isRequiredFullUpdate && notebookEvent is NotebookCellEvent && notebookEvent.isLastInBatch) {
      isRequiredFullUpdate = false
      refreshForAllCells()
    }
  }

  fun refreshForAllCells() {
    note.cells.forEach {
      addOrUpdateMarkerGuardBlock(it)
      addOrUpdateFolding(it)
    }
  }

  fun removeBlocksForCells() {
    val removingBlocksForCells = guardedMarkers.map { it.key }
    removingBlocksForCells.forEach {
      removeGuardBlock(it)
    }
  }

  private fun addOrUpdateMarkerGuardBlock(cell: NotebookCell) {
    val sourceRange = TextRange(cell.offset, cell.textOffset)
    val rangeMarker = guardedMarkers[cell]

    if (rangeMarker?.asTextRange == sourceRange)
      return

    if (rangeMarker != null)
      removeGuardBlock(cell)
    addGuardBlock(cell)
  }

  private fun addGuardBlock(cell: NotebookCell) {
    removeGuardBlock(cell)

    val newCellGuardMarker = editor.document.createGuardedBlock(cell.offset, cell.textOffset)
    newCellGuardMarker.putUserData(NotebookEditorUtils.READONLY_MARKER_REASON, "")
    newCellGuardMarker.isGreedyToLeft = true
    newCellGuardMarker.isGreedyToRight = false
    guardedMarkers[cell] = newCellGuardMarker
  }

  private fun removeGuardBlock(cell: NotebookCell) {
    val marker = guardedMarkers[cell] ?: return

    editor.document.removeGuardedBlock(marker)
    guardedMarkers.remove(cell)
  }

  private fun addOrUpdateFolding(cell: NotebookCell) {
    val sourceRange = TextRange(cell.offset, cell.textOffset)
    val startOffset = sourceRange.startOffset
    val endOffset = sourceRange.endOffset
    val fold = folds[cell]
    val isFoldCorrect = fold != null && fold.isValid &&
                        fold.asTextRange?.startOffset == startOffset &&
                        fold.asTextRange?.endOffset == endOffset &&
                        fold.getUserData(FOLD_MARKER_CELL_INDEX) == cell.indexInNote

    if (isFoldCorrect)
      return

    editor.foldingModel.runBatchFoldingOperation {
      fold?.let {
        it.putUserData(FOLD_MARKER_IS_REMOVING, true)
        editor.foldingModel.removeFoldRegion(it)
      }

      val foldingModel = editor.foldingModel as? FoldingModelImpl
      val sourceFoldRegion = foldingModel?.createFoldRegion(startOffset,
                                                            endOffset,
                                                            "",
                                                            null,
                                                            true) ?: return@runBatchFoldingOperation
      sourceFoldRegion.putUserData(FOLD_MARKER_CELL_INDEX, cell.indexInNote)

      folds[cell] = sourceFoldRegion
    }
  }

  private fun removeFolding(cell: NotebookCell) {
    val foldRegion = folds[cell] ?: return
    foldRegion.putUserData(FOLD_MARKER_IS_REMOVING, true)
    editor.foldingModel.runBatchFoldingOperation {
      editor.foldingModel.removeFoldRegion(foldRegion)
      folds.remove(cell)
    }
  }

  companion object {
    private val NOTE_CELL_CONTROLLER_KEY = Key<NoteCellsDelimiterController>("NOTE_CELL_CONTROLLER_KEY")
    private val FOLD_MARKER_CELL_INDEX = Key<Int>("FOLD_CELL_INDEX")
    private val FOLD_MARKER_IS_REMOVING = Key<Boolean>("FOLD_IS_REMOVING")

    fun getForEditor(editor: Editor): NoteCellsDelimiterController? = editor.getUserData(NOTE_CELL_CONTROLLER_KEY)
  }
}