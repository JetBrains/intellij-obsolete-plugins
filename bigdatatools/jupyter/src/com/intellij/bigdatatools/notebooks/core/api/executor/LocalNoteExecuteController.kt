package com.intellij.bigdatatools.notebooks.core.api.executor

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.notebooks.core.api.editor.NoteEditorActionListener
import com.intellij.bigdatatools.notebooks.core.api.editor.NotebookEditor
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.notebooks.core.impl.editor.external.ExternalNotebookModifier
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.rfs.driver.ConnectedConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.ConnectingConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.FailedConnectionStatus
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.bigdatatools.common.util.toPresentableText

class LocalNoteExecuteController(private val notebookEditor: NotebookEditor,
                                 private val executor: NoteExecutor) : Disposable {
  private val notebookModifier = ExternalNotebookModifier(notebookEditor.editor).also {
    Disposer.register(this, it)
  }

  private val project = notebookEditor.project
  val note = notebookEditor.note

  private val noteExecutorQueue = LocalNoteExecutorQueue(notebookEditor, executor)

  private val actionListener = object : NoteEditorActionListener {
    override fun runAll() = wrapAction {
      val cells = note.cells
      noteExecutorQueue.runCells(cells)
    }

    override fun stopAll() = wrapAction {
      noteExecutorQueue.stopAll()
    }

    override fun runCell(cell: NotebookCell) = wrapAction {
      noteExecutorQueue.runCells(listOf(cell))
    }

    override fun runCellGoBelow(cell: NotebookCell) = wrapAction {
      val index = cell.indexInNote + 1
      val cells = note.cells
      noteExecutorQueue.runCells(listOf(cell))

      if (index == cells.size) {
        val newCell = note.createCellFromText(cell.marker)
        notebookModifier.addCell(newCell, index)
      }
      invokeLater {
        NotebookEditorUtils.goToCell(notebookEditor.editor, note.cells[index])
      }
    }

    override fun stopCell(cell: NotebookCell) = wrapAction {
      noteExecutorQueue.stopCell(cell)
    }

    override fun runAllBelow(cell: NotebookCell) = wrapAction {
      val indexInNote = cell.indexInNote
      val cells = cell.note?.cells ?: return@wrapAction
      val cellsForExecute = cells.subList(indexInNote, cells.size)
      noteExecutorQueue.runCells(cellsForExecute)
    }

    override fun runAllAbove(cell: NotebookCell) = wrapAction {
      val indexInNote = cell.indexInNote
      val cells = cell.note?.cells ?: return@wrapAction
      val cellsForExecute = cells.subList(0, indexInNote)
      noteExecutorQueue.runCells(cellsForExecute)
    }

    override fun restartInterpreter(cell: NotebookCell) = wrapAction {
      executor.restartInterpreter(cell)
    }
  }


  init {
    notebookEditor.addActionListener(actionListener)
  }

  override fun dispose() = notebookEditor.removeActionListener(actionListener)

  @Suppress("HardCodedStringLiteral")
  private fun wrapAction(body: () -> Unit) = executeOnPooledThread {
    try {
      checkIsAvailable()
      body()
    }
    catch (t: PresentableException) {
      invokeLater {
        Messages.showErrorDialog(project, t.message, MessagesBundle.message("execution.error.title"))
      }
    }
    catch (t: Throwable) {
      invokeLater {
        Messages.showErrorDialog(project, t.toPresentableText(), MessagesBundle.message("execution.error.title"))
      }
    }
  }

  private fun checkIsAvailable(): Unit = when (executor.connectionStatus) {
    is FailedConnectionStatus -> throw Exception(
      MessagesBundle.message("local.note.action.error.driver.failed", executor.presentableName))
    is ConnectingConnectionStatus -> throw Exception(
      MessagesBundle.message("local.note.action.error.driver.connecting", executor.presentableName))
    is ConnectedConnectionStatus -> Unit
  }
}