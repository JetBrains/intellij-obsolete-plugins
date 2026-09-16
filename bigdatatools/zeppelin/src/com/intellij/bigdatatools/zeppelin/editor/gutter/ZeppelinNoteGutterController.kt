package com.intellij.bigdatatools.zeppelin.editor.gutter

import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellAdded
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellRemoved
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NoteChangeEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter

class ZeppelinNoteGutterController(val zeppelinEditor: ZeppelinEditor) : Disposable {
  private val note = zeppelinEditor.note
  private val markupModel: MarkupModel = zeppelinEditor.editor.markupModel

  private val cellsToHighlighter = mutableMapOf<NotebookCell, RangeHighlighter>()

  private val listener = object : NotebookChangeListener {
    var isRequiredFullUpdate = false
    override fun onEvent(notebookEvent: NotebookEvent) {
      when (notebookEvent) {
        is CellAdded -> {
          isRequiredFullUpdate = true
        }
        is CellRemoved -> {
          isRequiredFullUpdate = true
          removeGutter(notebookEvent.cell)
        }
        is CellChanged -> {
          val affectedGutterFields = setOf(NotebookSchema.cellStatus, NotebookSchema.cellSource,
                                           NotebookSchema.cellText)
          if (notebookEvent.changedFields.intersect(affectedGutterFields).isEmpty())
            return

          val zeppelinCell = (notebookEvent.cell as? ZeppelinCell) ?: return
          if (!isRequiredFullUpdate)
            addOrUpdateGutter(zeppelinCell)
        }
        is NoteChangeEvent -> {
          isRequiredFullUpdate = true
        }
      }
      if (isRequiredFullUpdate && notebookEvent.isLastInBatch) {
        isRequiredFullUpdate = false
        updateAll()
      }
    }
  }

  init {
    note.addNotebookChangeListener(listener)
    updateAll()
  }

  override fun dispose() {
    note.removeNotebookChangeListener(listener)
  }

  private fun updateAll() = note.cells.forEach {
    addOrUpdateGutter(it)
  }

  private fun addOrUpdateGutter(cell: ZeppelinCell) {
    val oldGutter = cellsToHighlighter[cell]
    when {
      oldGutter == null -> {
        addGutter(cell)
      }
      oldGutter.startOffset != cell.offset -> {
        removeGutter(cell)
        addGutter(cell)
      }
    }

    removeGutter(cell)
    addGutter(cell)
  }

  private fun addGutter(cell: ZeppelinCell) {
    val sourceRange = NotebookEditorUtils.getCellRangeInEditor(zeppelinEditor.editor, cell)

    val rangeHighlighter = markupModel.addRangeHighlighter(sourceRange.startOffset, sourceRange.endOffset, HighlighterLayer.ERROR + 1, null,
                                                           HighlighterTargetArea.LINES_IN_RANGE)
    rangeHighlighter.gutterIconRenderer = ZeppelinRunGutterIconRender(cell, zeppelinEditor)
    cellsToHighlighter[cell] = rangeHighlighter
  }


  private fun removeGutter(cell: NotebookCell) {
    val rangeHighlighter = cellsToHighlighter.remove(cell) ?: return
    markupModel.removeHighlighter(rangeHighlighter)
  }
}