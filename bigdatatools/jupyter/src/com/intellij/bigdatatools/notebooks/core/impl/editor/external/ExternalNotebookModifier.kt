package com.intellij.bigdatatools.notebooks.core.impl.editor.external

import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookOutput
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ReadOnlyModificationException
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import java.lang.invoke.MethodHandles

/**
 * Provide an ability for external modifications of a notebook.
 *
 * During this modifications editor, psi, virtual file and model will be updated.
 * Listeners of this class catch all modifications of notebook, except own modifications
 */
class ExternalNotebookModifier(val editor: Editor) : Disposable {
  private val project: Project = editor.project ?: throw Exception("Project is not found")
  private val notebookFile: NotebookVirtualFile = FileDocumentManager.getInstance().getFile(editor.document) as? NotebookVirtualFile
                                                  ?: throw Exception("Notebook Virtual File is not found")

  private val listeners = mutableListOf<NotebookChangeListener>()
  fun addNotebookChangeListener(notebookChangeListener: NotebookChangeListener) = listeners.add(notebookChangeListener)
  fun removeNotebookChangeListener(notebookChangeListener: NotebookChangeListener) = listeners.remove(notebookChangeListener)

  private val notebookModelListener = object : NotebookChangeListener {
    override fun onEvent(notebookEvent: NotebookEvent) {
      if (ignoreModelListeners) return
      listeners.forEach {
        try {
          it.onEvent(notebookEvent)
        }
        catch (e: Exception) {
          logger.error(e)
        }
      }
    }
  }

  private val editorManager = NotebookDocumentModifier(editor)
  var notebook = notebookFile.notebook
    private set
  private var ignoreModelListeners: Boolean = false

  init {
    notebook.addNotebookChangeListener(notebookModelListener)
  }

  override fun dispose() = destroy()

  private fun destroy() {
    listeners.clear()
    notebook.removeNotebookChangeListener(notebookModelListener)
  }

  fun replaceNotebook(newNotebook: BasicNotebook, isUndoable: Boolean) = runTransaction {
    editorManager.replaceNotebook(newNotebook, isUndoable)
  }

  fun addCell(cell: NotebookCell, index: Int) = runTransaction {
    addCellNonBlock(cell, index)
  }

  fun setOutput(cell: NotebookCell, output: NotebookOutput?) = runTransaction {
    cell.setOutput(output)
  }

  fun moveCell(cell: NotebookCell, toIndex: Int) = runTransaction {
    val sourceIndex = cell.indexInNote
    if (sourceIndex == -1) {
      logger.warn("Try to move the nonexistent cell from notebook")
      return@runTransaction
    }
    if (!editor.document.isWritable) {
      logger.warn("Try to move cell from read only document")
      return@runTransaction
    }
    editorManager.moveCell(cell, toIndex)
  }

  fun removeCell(cell: NotebookCell) = runTransaction {
    val index = cell.indexInNote
    if (index == -1) {
      logger.warn("Try to remove the nonexistent cell from notebook")
      return@runTransaction
    }
    if (!editor.document.isWritable) {
      logger.warn("Try to remove cell from read only document")
      return@runTransaction
    }
    removeCellInner(index)
  }

  fun removeCell(index: Int) = runTransaction {
    removeCellInner(index)
  }

  fun changeSyncStatus(cell: NotebookCell, isSynced: Boolean) = runTransaction {
    cell.changeSyncStatus(isSynced)
  }

  private fun addCellNonBlock(cell: NotebookCell, index: Int) = editorManager.addCell(cell, index)

  private fun removeCellInner(index: Int) {
    if (notebook.cells.size <= index || index < 0)
      throw IndexOutOfBoundsException()

    if (notebook.cells.size == 1) {
      editorManager.clearCell(0)
    }
    else {
      editorManager.removeCell(index)
    }

  }

  fun updateCell(index: Int, cell: NotebookCell) = runTransaction {
    editorManager.updateCell(cell, index)
  }

  fun updateCell(oldCell: NotebookCell, newCell: NotebookCell) = runTransaction {
    val index = oldCell.indexInNote
    if (index == -1) {
      logger.warn("Changing cell is not exists, source: ${oldCell.source}")
      return@runTransaction
    }
    editorManager.updateCell(newCell, index)
  }

  fun <T> runTransaction(body: () -> T): T? {
    var res: T? = null
    invokeAndWaitIfNeeded {
      withIgnoreListeners {
        try {
          notebook.performModification {
            if (!isWriteAllowed()) throw ReadOnlyModificationException(editor.document, null)
            res = body()
          }
        }
        catch (t: Throwable) {
          NotebookEditorUtils.handleError(project, notebookFile, t)
          throw t
        }
      }
    }
    return res
  }

  private fun isWriteAllowed(): Boolean {
    val status = ReadonlyStatusHandler.getInstance(project)
      .ensureFilesWritable(listOf(notebookFile.originFile))
    return if (status.hasReadonlyFiles()) {
      HintManager.getInstance().showInformationHint(editor, "<html>${status.readonlyFilesMessage}</html>")
      false
    }
    else true
  }

  private fun <T> withIgnoreListeners(body: () -> T): T {
    assert(!ignoreModelListeners) {
      "Multi ignore operation in one change"
    }
    try {
      ignoreModelListeners = true
      return body()
    }
    finally {
      ignoreModelListeners = false
    }
  }

  companion object {
    private val logger = Logger.getInstance(MethodHandles.lookup().lookupClass())
  }
}