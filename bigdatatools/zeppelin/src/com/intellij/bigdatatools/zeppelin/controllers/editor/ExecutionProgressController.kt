package com.intellij.bigdatatools.zeppelin.controllers.editor

import com.intellij.bigdatatools.notebooks.core.api.editor.NoteEditorActionListener
import com.intellij.bigdatatools.notebooks.core.api.executor.NoteExecutionListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinNoteLoadPromise.onNoteLoaded
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.editor.ui.ExecutionProgressPanel
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsNoteController.Companion.isDebugCell
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.jetbrains.bigdatatools.common.util.invokeLater
import kotlin.properties.Delegates

class ExecutionProgressController(private val zeppelinEditor: ZeppelinEditor) : NoteExecutionListener, Disposable {
  val editor = zeppelinEditor.editor
  val note: ZeppelinNotebook = zeppelinEditor.note

  // We need to disable execution progress for Zeppelin <=7
  var disabled: Boolean by Delegates.observable(false) { _, _, newValue ->
    if (newValue)
      executionProgress.hideAll()
  }

  private val executionProgress = ExecutionProgressPanel()
  private var finishedCellsCount = 0
  private var plannedCellsCount = 0
  private var lastCellIndex = -1

  private val noteListener = object : NotebookChangeListener {
    override fun onEvent(notebookEvent: NotebookEvent) {
      if (disabled) return
      if (notebookEvent !is CellChanged) return
      if (!notebookEvent.changedFields.contains(NotebookSchema.cellStatus)) return

      val cell = notebookEvent.cell as ZeppelinCell

      val status = cell.status
      if (!cell.isDebugCell) {
        if (shouldIgnoreChanges(cell.status))
          return

        executionProgress.hideZtools()
        val cellIndexInNote = if (!isNoteContainsZtoolsCell())
          cell.indexInNote
        else
          cell.indexInNote - 1
        updateByStatus(status, cellIndexInNote)
      }
      else {
        updateZtoolsStatus(status)
      }

      executionProgress.repaint()
    }
  }

  private val actionListener = object : NoteEditorActionListener {
    override fun addPlanningExecutingCellsFromLocalQueue(cells: List<NotebookCell>) {
      if (disabled) return

      onCellRun(cells)
    }

    override fun runAll() {
      if (disabled) return

      onCellRun(note.cells)
    }

    override fun runCell(cell: NotebookCell) {
      if (disabled) return

      onCellRun(listOf(cell))
    }
  }

  init {
    note.addNotebookChangeListener(noteListener)
    zeppelinEditor.addActionListener(actionListener)
    executionProgress.setLinkLabelAction { goToCell() }
    refreshStatus()

    invokeLater {
      //I add invokeLater here because noteLoadPromise is not inited yet
      zeppelinEditor.onNoteLoaded {
        refreshStatus()
      }
    }
  }

  override fun dispose() {
    note.removeNotebookChangeListener(noteListener)
    zeppelinEditor.removeActionListener(actionListener)
  }

  private fun refreshStatus() {
    // Find and show first running or first error cell.
    val cell = note.cells.find { it.isLaunched }
               ?: note.cells.find { it.isErrorOrAbort }
               ?: return

    updateByStatus(cell.status, cell.indexInNote)
  }

  private fun isNoteContainsZtoolsCell(): Boolean = note.cells.first().isDebugCell

  private fun updateZtoolsStatus(status: CellStatus) {
    stopExecution()
    when (status) {
      CellStatus.READY -> {
      }
      CellStatus.PENDING, CellStatus.RUNNING -> executionProgress.setZtoolsText(
        ZepMessagesBundle.message("execution.progress.ztools.syncing"))
      CellStatus.FINISHED -> executionProgress.setZtoolsText(ZepMessagesBundle.message("execution.progress.ztools.synced"))
      CellStatus.ABORT, CellStatus.ERROR -> executionProgress.setZtoolsText(ZepMessagesBundle.message("execution.progress.ztools.error"))
      else -> {}
    }
  }

  private fun updateByStatus(status: CellStatus, cellIndexInNote: Int) {
    when (status) {
      CellStatus.READY -> Unit
      CellStatus.PENDING -> {
        val isRunning = note.cells.any { it.status == CellStatus.RUNNING }
        if (!isRunning) {
          executionProgress.setProgress(0)
          updateLabels(status, cellIndexInNote)
        }
      }
      CellStatus.RUNNING -> {
        executionProgress.setProgress(0)
        updateLabels(status, cellIndexInNote)
      }
      CellStatus.FINISHED -> {
        finishedCellsCount++
        if (isAllFinished()) stopExecution()
        updateLabels(status, cellIndexInNote)
      }
      CellStatus.ABORT -> {
        val isAnyRunning = note.cells.any { it.status == CellStatus.RUNNING }
        if (!isAnyRunning) {
          stopExecution()
          updateLabels(status, cellIndexInNote)
        }
      }
      CellStatus.ERROR -> {
        stopExecution()
        updateLabels(status, cellIndexInNote)
      }
      else -> {}
    }
  }

  private fun updateLabels(status: CellStatus, cellIndexInNote: Int) {
    lastCellIndex = cellIndexInNote
    updateStatusLabel(status)
    updateLinkLabel()
  }

  private fun isAllFinished() = finishedCellsCount == plannedCellsCount || plannedCellsCount == 0

  private fun stopExecution() {
    executionProgress.hideProgress()
    plannedCellsCount = 0
  }

  private fun updateLinkLabel() {
    if (plannedCellsCount == 0) {
      executionProgress.setLinkLabel(lastCellIndex)
    }
    else {
      executionProgress.setLinkLabel(finishedCellsCount + 1, plannedCellsCount)
    }
  }

  private fun goToCell() {
    val cells = note.cells

    val realIndex = if (!isNoteContainsZtoolsCell())
      lastCellIndex
    else
      lastCellIndex + 1

    if (realIndex < -1 || cells.isEmpty()) return

    val cell = if (realIndex >= cells.size)
      note.cells[cells.size - 1]
    else
      note.cells[realIndex]

    val range = cell.textRange
    val position = editor.offsetToLogicalPosition(range.startOffset)
    editor.scrollingModel.scrollTo(position, ScrollType.CENTER)
    editor.caretModel.moveToOffset(range.startOffset)
  }

  fun onCellRun(cells: List<NotebookCell>) {
    finishedCellsCount = 0
    plannedCellsCount = cells.count {
      !it.isWithoutBody()
    }

    if (plannedCellsCount == 1) {
      plannedCellsCount = 0
    }
  }

  override fun onProgress(editor: Editor, cell: NotebookCell, percentage: Int) {
    if (disabled || (cell as ZeppelinCell).isDebugCell) return

    executionProgress.setProgress(percentage)
  }

  fun getComponent(): ExecutionProgressPanel = executionProgress

  private fun updateStatusLabel(status: CellStatus) {
    when (status) {
      CellStatus.PENDING -> {
        executionProgress.setIcon(null)
        executionProgress.setLabel(ZepMessagesBundle.message("execution.progress.pending"))
      }
      CellStatus.RUNNING -> {
        executionProgress.setIcon(null)
        executionProgress.setLabel(ZepMessagesBundle.message("execution.progress.running"))
      }
      CellStatus.ABORT -> {
        val isAnyRunning = note.cells.any { it.isLaunched }
        if (isAnyRunning)
          return
        executionProgress.setIcon(AllIcons.RunConfigurations.ToolbarTerminated)
        executionProgress.setLabel(ZepMessagesBundle.message("execution.progress.aborted"))
      }
      CellStatus.ERROR -> {
        executionProgress.setIcon(AllIcons.RunConfigurations.ToolbarError)
        executionProgress.setLabel(ZepMessagesBundle.message("execution.progress.error"))
      }
      CellStatus.FINISHED -> {
        if (!isAllFinished()) return
        executionProgress.setIcon(AllIcons.RunConfigurations.ToolbarPassed)
        executionProgress.setLabel(ZepMessagesBundle.message("execution.progress.finished"))

      }
      CellStatus.READY -> {
        executionProgress.setIcon(null)
        executionProgress.setLabel("")
      }
      else -> {}
    }
  }

  private fun shouldIgnoreChanges(status: CellStatus): Boolean =
    status == CellStatus.READY ||
    status == CellStatus.PENDING && note.cells.any { it.status == CellStatus.RUNNING }

  private val ZeppelinCell.isErrorOrAbort: Boolean
    get() = status == CellStatus.ABORT || status == CellStatus.ERROR
}