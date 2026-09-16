package com.intellij.bigdatatools.notebooks.core.api.executor

import com.intellij.bigdatatools.notebooks.core.api.editor.NotebookEditor
import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookOutput
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultMessage
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.OutputCode
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.bigdatatools.common.util.toPresentableText
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class LocalNoteExecutorQueue(val editor: NotebookEditor, private val noteExecutor: NoteExecutor) : Disposable {
  private val note: BasicNotebook = editor.note

  private val isDisposed = AtomicBoolean(false)
  private val cellQueue = CopyOnWriteArrayList<NotebookCell>()
  private var curExecutingCell: NotebookCell? = null

  private val notebookChangeListener = object : NotebookChangeListener {
    override fun onEvent(notebookEvent: NotebookEvent) {
      if (notebookEvent is CellChanged &&
          NotebookSchema.cellStatus in notebookEvent.changedFields &&
          notebookEvent.cell.status.isFinished) {
        runNextCell()
      }
    }
  }

  private val listener = object : NoteExecutorListener {
    override fun onConnect() {}

    override fun onConnectionError(t: Throwable) {
      abortCells(t)
    }

    override fun onCellProgress(cell: NotebookCell, percentage: Int) {
      if (cell.status.isFinished)
        return

      notifyNoteExecutionListeners {
        it.onProgress(editor.editor, cell, percentage)
      }
    }

    override fun onCellResult(cell: NotebookCell, status: CellStatus, output: NotebookOutput?) {
      cell.note?.performModification {
        cell.status = status
        output?.let { cell.setOutput(output) }
        if (cell.status.isFinished) {
          cell.dateFinished = Date()
          cell.dateUpdated = Date()
        }
      }

      if (cell.status.isFinished && cell == curExecutingCell) {
        curExecutingCell = null
      }
    }

    override fun onCellOutput(cell: NotebookCell, data: String, index: Int, type: CellResultType?, update: Boolean) {
      if (cell.status.isFinished)
        return

      notifyNoteExecutionListeners {
        it.onOutput(editor.editor, cell, data, index, type ?: CellResultType.TEXT, update)
      }
    }


    override fun onCellInfo(cell: NotebookCell, info: Map<String, Any>) = notifyNoteExecutionListeners {
      it.onParagraphInfo(editor.editor, cell, info)
    }
  }

  init {
    note.addNotebookChangeListener(notebookChangeListener)
    noteExecutor.addListener(listener)
  }

  override fun dispose() {
    isDisposed.set(true)
    noteExecutor.removeListener(listener)
    note.removeNotebookChangeListener(notebookChangeListener)
    abortCells(Throwable("Executor had been disposed"))
  }


  fun runCells(cellsForExecute: List<NotebookCell>) {
    if (isDisposed.get())
      return

    val cells = cellsForExecute.filterNot { it.isWithoutBody() }
    cells.forEach {
      cellQueue.add(it)
      setPendingStatus(it)
    }
    runNextCell()
  }


  fun stopCell(cell: NotebookCell?) {
    if (isDisposed.get())
      return

    if (cell == null)
      return
    doExecutorStopCell(cell)
    if (cell == curExecutingCell)
      curExecutingCell = null
    cellQueue.remove(cell)
    setAbortStatus(cell)
  }

  fun stopAll() {
    if (isDisposed.get())
      return

    cellQueue.clear()

    stopCell(curExecutingCell)
  }

  private fun doExecutorStopCell(cell: NotebookCell) {
    if (cell == curExecutingCell) {
      noteExecutor.stopCell(cell)
    }
  }

  private fun runNextCell() {
    if (isDisposed.get())
      return

    if (curExecutingCell == null || curExecutingCell?.status?.isFinished == true) {
      val nextCell = cellQueue.firstOrNull()
      cellQueue.remove(nextCell)

      nextCell?.let {
        it.note?.performModification {
          it.dateStarted = Date()
          it.dateUpdated = Date()
          it.setOutput(null)
        }
        curExecutingCell = it
        noteExecutor.runCell(it)
      }
    }
  }

  private fun abortCells(t: Throwable?) {
    curExecutingCell?.let {
      if (t != null) {
        val msg = listOf(CellResultMessage(CellResultType.TEXT, "Aborted. Reason ${t.toPresentableText()}"))
        listener.onCellResult(it, CellStatus.ABORT, NotebookOutput(OutputCode.ERROR, msg))
      }
      else {
        setAbortStatus(it)
      }
    }
    cellQueue.map { setAbortStatus(it) }
  }


  private fun setAbortStatus(cell: NotebookCell) {
    cell.note?.performModification {
      cell.status = CellStatus.ABORT
    }
  }


  private fun setPendingStatus(cell: NotebookCell) {
    cell.note?.performModification {
      cell.status = CellStatus.PENDING
    }
  }

  private fun notifyNoteExecutionListeners(body: (NoteExecutionListener) -> Unit) =
    noteExecutor.getNoteExecutionListeners().forEach {
      try {
        body(it)
      }
      catch (t: Throwable) {
        logger.warn(t)
      }
    }

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}