package com.intellij.bigdatatools.zeppelin.controllers.editor

import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellAdded
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellRemoved
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.style.ZeppelinStyleSettings
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.ex.FoldingListener
import com.intellij.openapi.editor.ex.FoldingModelEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange

class ZeppelinParagraphFoldingController(private val editor: Editor,
                                         private val note: BasicNotebook) : NotebookChangeListener, FoldingListener, Disposable {

  private val folds = HashMap<NotebookCell, FoldRegion>()

  private var isRequiredFullUpdate = false

  var isCellsHidden = false
    private set

  init {
    note.cells.forEach { cell -> addOrUpdateFolding(cell) }
    note.addNotebookChangeListener(this)
    (editor.foldingModel as? FoldingModelEx)?.addListener(this, this)

    editor.putUserData(FOLDING_CONTROLLER_KEY, this)
  }

  fun hideAllCells() {
    if (isCellsHidden) {
      return
    }

    val foldingModel = editor.foldingModel as? FoldingModelEx ?: return

    foldingModel.runBatchFoldingOperation {

      note.cells.forEach { cell ->

        removeFolding(cell)

        val sourceRange = NotebookEditorUtils.getCellRangeInEditor(editor, cell)

        val fold = foldingModel.createFoldRegion(sourceRange.startOffset, sourceRange.endOffset, "", null, true) ?: return@forEach

        fold.putUserData(FOLD_CELL_INDEX, cell.indexInNote)
        fold.putUserData(FOLD_CELL_TITLE, cell.title)

        folds[cell] = fold
      }
    }

    isCellsHidden = true
  }

  fun showAllCells() {

    if (!isCellsHidden) {
      return
    }

    editor.foldingModel.runBatchFoldingOperation {
      note.cells.forEach {
        removeFolding(it)
        addOrUpdateFolding(it)
      }
    }

    isCellsHidden = false
  }

  override fun onFoldRegionStateChange(region: FoldRegion) {
    //Folding make removing fold expanded first and after that remove it, we need pass this situations
    if (region.getUserData(FOLD_IS_REMOVING) == true)
      return

    val cell = folds.entries.firstOrNull { it.value == region }?.key as? ZeppelinCell ?: return
    if (cell.editorHide == !region.isExpanded) return
    cell.note.performModification {
      cell.editorHide = !region.isExpanded
    }
  }

  override fun dispose() {
    note.removeNotebookChangeListener(this)
    editor.putUserData(FOLDING_CONTROLLER_KEY, null)
  }

  override fun onEvent(notebookEvent: NotebookEvent) {
    when (notebookEvent) {
      is CellAdded -> {
        isRequiredFullUpdate = true
      }
      is CellRemoved -> {
        isRequiredFullUpdate = true
        editor.foldingModel.runBatchFoldingOperation {
          removeFolding(notebookEvent.cell)
        }
      }
      is CellChanged ->
        if (!isRequiredFullUpdate) {
          val isChangeRequires = notebookEvent.changedFields.any { it in foldingAffectedFields }
          if (isChangeRequires && !isFoldCorrect(notebookEvent.cell)) {
            isRequiredFullUpdate = true
          }
        }
    }
    if (isRequiredFullUpdate && notebookEvent is NotebookCellEvent && notebookEvent.isLastInBatch) {
      isRequiredFullUpdate = false
      note.cells.forEach {
        addOrUpdateFolding(it)
      }
    }
  }

  fun removeFolding() {
    editor.foldingModel.runBatchFoldingOperation {
      folds.forEach {
        it.value.putUserData(FOLD_IS_REMOVING, true)
        editor.foldingModel.removeFoldRegion(it.value)
      }
      folds.clear()
    }
  }

  private fun removeFolding(cell: NotebookCell) {
    folds[cell]?.let {
      it.putUserData(FOLD_IS_REMOVING, true)
      editor.foldingModel.removeFoldRegion(it)
      folds.remove(cell)
    }
  }

  private fun isFoldCorrect(cell: NotebookCell): Boolean {
    return isFoldCorrect(folds[cell], NotebookEditorUtils.getCellRangeInEditor(editor, cell), cell)
  }

  private fun isFoldCorrect(fold: FoldRegion?, sourceRange: TextRange, cell: NotebookCell): Boolean {
    return fold != null && fold.isValid &&
           fold.textRange.startOffset == sourceRange.startOffset &&
           fold.textRange.endOffset == sourceRange.endOffset &&
           fold.getUserData(FOLD_CELL_TITLE) == cell.title &&
           fold.getUserData(FOLD_CELL_INDEX) == cell.indexInNote
  }

  private fun addOrUpdateFolding(cell: NotebookCell) {
    val fold = folds[cell]
    val sourceRange = NotebookEditorUtils.getCellRangeInEditor(editor, cell)
    val isFoldCorrect = isFoldCorrect(fold, sourceRange, cell)
    if (isFoldCorrect) return

    editor.foldingModel.runBatchFoldingOperation {
      fold?.let {
        it.putUserData(FOLD_IS_REMOVING, true)
        editor.foldingModel.removeFoldRegion(it)
      }

      val editorHide = (cell as? ZeppelinCell)?.editorHide

      val interpreter = ((cell as? ZeppelinCell)?.interpreterCode ?: "").ifEmpty { "default interpreter" }

      if (sourceRange.length == 1) {
        folds.remove(cell)
        return@runBatchFoldingOperation
      }

      val collapsedParagraphMessage = ZepMessagesBundle.message("folding.collapsed.prefix", interpreter)
      val placeholderText = " $collapsedParagraphMessage ${cell.indexInNote + 1}"

      val sourceFoldRegion = editor.foldingModel.addFoldRegion(sourceRange.startOffset, sourceRange.endOffset, placeholderText)
                             ?: return@runBatchFoldingOperation

      sourceFoldRegion.isExpanded = editorHide != true
      sourceFoldRegion.putUserData(FOLD_CELL_INDEX, cell.indexInNote)
      sourceFoldRegion.putUserData(FOLD_CELL_TITLE, cell.title)

      folds[cell] = sourceFoldRegion
    }
  }

  companion object {
    private val FOLD_CELL_INDEX = Key<Int>("FOLD_CELL_INDEX")
    private val FOLD_CELL_TITLE = Key<String?>("FOLD_CELL_TITLE")
    private val FOLD_IS_REMOVING = Key<Boolean>("FOLD_IS_REMOVING")

    val FOLDING_CONTROLLER_KEY = Key<ZeppelinParagraphFoldingController>("ZeppelinParagraphFoldingController")

    // List of field change of which, can trigger folding update.
    private val foldingAffectedFields = setOf(NotebookSchema.cellSource,
                                              NotebookSchema.title,
                                              NotebookSchema.cellConfig)

    fun update(editor: Editor, note: BasicNotebook) {

      val currentFoldingController = editor.getUserData(FOLDING_CONTROLLER_KEY)

      if (ZeppelinStyleSettings.getInstance().cellsFolding) {
        if (currentFoldingController == null) {
          Disposer.register((editor as EditorImpl).disposable, ZeppelinParagraphFoldingController(editor, note))
        }
      }
      else {
        if (currentFoldingController != null) {
          currentFoldingController.removeFolding()
          Disposer.dispose(currentFoldingController)
        }
      }
    }
  }
}