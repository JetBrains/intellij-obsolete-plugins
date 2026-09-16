package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.notebooks.core.api.NotebookConstants
import com.intellij.bigdatatools.notebooks.core.api.NotebookDataKeys
import com.intellij.bigdatatools.notebooks.core.api.editor.NoteEditorActionListener
import com.intellij.bigdatatools.notebooks.core.api.editor.NotebookEditor
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.notebooks.core.impl.editor.getNotebookCellByOffset
import com.intellij.bigdatatools.notebooks.core.impl.editor.getOffsetOfCaretStartLine
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.notebooks.style.NoteStyleSettings
import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.jetbrains.bigdatatools.common.editor.BdiDecoratableEditor

class NotebookEditorActionService {
  fun renameCellTitle(event: AnActionEvent) = notifyCellAction(event) { listener, cell ->
    listener.changeCellTitleIsVisible(cell)
  }

  fun runCell(event: AnActionEvent) = notifyCellAction(event) { listener, cell ->
    listener.runCell(cell)
  }

  fun runCellAndGoBelow(event: AnActionEvent) = notifyCellAction(event) { listener, cell ->
    listener.runCellGoBelow(cell)
  }

  fun runAllBelow(event: AnActionEvent) = notifyCellAction(event) { listener, cell ->
    listener.runAllBelow(cell)
  }

  fun runAllAbove(event: AnActionEvent) = notifyCellAction(event) { listener, cell ->
    listener.runAllAbove(cell)
  }

  fun stopCell(event: AnActionEvent) = notifyCellAction(event) { listener, cell ->
    listener.stopCell(cell)
  }

  fun clearAllOutputs(event: AnActionEvent) {
    val (project, _, notebookVirtualFile) = getNotebookExecutionData(event) ?: return
    val noteEditor = NotebookEditorUtils.getNotebookEditor(project, notebookVirtualFile)
    noteEditor?.actionNotify {
      it.clearAllOutput()
    }
  }

  fun clearCellOutput(e: AnActionEvent) = notifyCellAction(e) { listener, cell ->
    listener.clearCellOutput(cell)
  }

  fun restartInterpreter(event: AnActionEvent) = notifyCellAction(event) { listener, cell ->
    listener.restartInterpreter(cell)
  }

  fun cloneCell(e: AnActionEvent) = notifyCellAction(e) { listener, cell ->
    listener.cloneCell(cell, cell.indexInNote + 1)
  }


  fun deleteCell(event: AnActionEvent) {
    val (project, cell, notebookVirtualFile) = getNotebookExecutionData(event) ?: return
    val notebookEditor = getNoteEditor(project, notebookVirtualFile) ?: return

    if (cell.note?.cells?.size == 1) {
      Messages.showWarningDialog(notebookEditor.project,
                                 NoteMessagesBundle.message("action.delete.single.cell.msg"),
                                 NoteMessagesBundle.message("action.delete.single.cell.title"))
      return
    }

    if (!NotificationUtils.showCheckConfirmationDialog({ NoteStyleSettings.getInstance().needConfirmCellDelete },
                                                          { value -> NoteStyleSettings.getInstance().needConfirmCellDelete = value },
                                                          NoteMessagesBundle.message("action.delete.confirmation.title"),
                                                          NoteMessagesBundle.message("action.delete.confirmation.msg"),
                                                          NoteMessagesBundle.message("command.delete"))) {
      return
    }

    notebookEditor.actionNotify {
      it.deleteCell(cell)
    }
  }

  fun hasCellAbove(event: AnActionEvent): Boolean {
    val cell = event.noteCell ?: return false
    return cell.indexInNote > 0
  }

  fun hasCellBelow(event: AnActionEvent): Boolean {
    val cell = event.noteCell ?: return false
    val note = cell.note ?: return false
    val indexInNote = cell.indexInNote
    return indexInNote >= 0 && indexInNote < note.cells.size - 1
  }

  fun moveCell(event: AnActionEvent, destination: RelativeDestination) {
    val cell = event.noteCell ?: error("Cell for action not found")
    val notebookVirtualFile = event.notebookVirtualFile ?: error("NotebookVirtualFile is not found")

    val targetIndex = cell.indexInNote.let {
      if (destination == RelativeDestination.BELOW)
        it + 1
      else
        it - 1
    }

    if (targetIndex < 0 || targetIndex >= notebookVirtualFile.notebook.cells.size)
      return

    val project = event.project ?: return
    val noteEditor = NotebookEditorUtils.getNotebookEditor(project, notebookVirtualFile)
    noteEditor?.actionNotify {
      it.moveCell(cell, targetIndex)
    }
  }

  fun splitCell(event: AnActionEvent) {
    val cell = event.noteCell ?: error("Cell for action not found")
    val notebookVirtualFile = event.notebookVirtualFile ?: error("NotebookVirtualFile is not found")
    val lineOffset = event.lineOffset ?: error("Line offset is not found")
    val project = event.project ?: return
    val noteEditor = NotebookEditorUtils.getNotebookEditor(project, notebookVirtualFile)

    if (!NotificationUtils.showCheckConfirmationDialog({ NoteStyleSettings.getInstance().needConfirmCellSplit },
                                                          { value -> NoteStyleSettings.getInstance().needConfirmCellSplit = value },
                                                          NoteMessagesBundle.message("action.split.confirmation.title"),
                                                          NoteMessagesBundle.message("action.split.confirmation.msg"),
                                                          NoteMessagesBundle.message("command.split"))) {
      return
    }

    noteEditor?.actionNotify {
      it.splitCell(cell, lineOffset)
    }
  }

  fun isSplitCellAvailable(event: AnActionEvent): Boolean {
    val cell = event.noteCell ?: error("Cell for action not found")
    val editor = event.noteEditor ?: error("Notebook editor is not found")
    val offset = event.lineOffset ?: error("Line offset is not found")

    val line = editor.document.getLineNumber(offset)
    val cellStartLine = editor.document.getLineNumber(cell.textOffset)

    return line != cellStartLine
  }

  fun mergeCellWithNext(event: AnActionEvent) {
    val cell = event.noteCell ?: error("Cell for action not found")
    val notebookVirtualFile = event.notebookVirtualFile ?: error("NotebookVirtualFile is not found")
    val project = event.project ?: return
    val noteEditor = NotebookEditorUtils.getNotebookEditor(project, notebookVirtualFile)

    if (!NotificationUtils.showCheckConfirmationDialog({ NoteStyleSettings.getInstance().needConfirmCellMerge },
                                                          { value -> NoteStyleSettings.getInstance().needConfirmCellMerge = value },
                                                          NoteMessagesBundle.message("action.merge.confirmation.title"),
                                                          NoteMessagesBundle.message("action.merge.confirmation.msg"),
                                                          NoteMessagesBundle.message("command.merge"))) {
      return
    }

    noteEditor?.actionNotify {
      it.mergeWithNext(cell)
    }
  }

  fun insertNewEmptyCell(event: AnActionEvent, destination: RelativeDestination) {
    val cell = event.noteCell ?: throw Exception("Cell for action not found")
    val notebookFile = event.notebookVirtualFile ?: throw Exception("NotebookVirtualFile is not found")

    val cellIndex = cell.indexInNote.let {
      if (destination == RelativeDestination.BELOW)
        it + 1
      else
        it
    }

    val notebook = notebookFile.notebook

    val prevCell = if (cellIndex >= 1)
      notebook.cells[cellIndex - 1]
    else
      notebook.cells[0]

    val textMarker = if (prevCell.interpreterCode.isNotBlank()) {
      NotebookConstants.INTERPRETER_MARKER + prevCell.interpreterCode + "\n"
    }
    else {
      ""
    }

    val project = event.project ?: return

    addEmptyCell(project, notebookFile, cellIndex, textMarker)
  }

  private fun addEmptyCell(project: Project, notebookVirtualFile: NotebookVirtualFile, cellIndex: Int, textMarker: String) {
    val noteEditor = NotebookEditorUtils.getNotebookEditor(project, notebookVirtualFile)
    noteEditor?.actionNotify {
      it.addCell(cellIndex, textMarker)
    }
  }

  fun goToNextCell(event: AnActionEvent) {
    val editor = event.noteEditor ?: return
    val curCell = event.noteCell ?: return
    val nextCell = getNextCell(curCell) ?: return

    NotebookEditorUtils.goToCell(editor, nextCell)
  }

  fun goToPrevCell(event: AnActionEvent) {
    val editor = event.noteEditor ?: return
    val curCell = event.noteCell ?: return
    val prevCell = getPrevCell(curCell) ?: return

    NotebookEditorUtils.goToCell(editor, prevCell)
  }

  fun hasRunningCells(event: AnActionEvent): Boolean {
    val project = event.project ?: return false
    val selectedEditor = FileEditorManager.getInstance(project).selectedEditor as? NotebookEditor ?: return false
    return selectedEditor.note.hasRunningCells
  }

  private fun getPrevCell(cell: NotebookCell): NotebookCell? {
    val indexInNote = cell.indexInNote
    val cells = cell.note?.cells ?: return null
    if (indexInNote == 0)
      return null

    return cells[indexInNote - 1]
  }

  private fun getNextCell(cell: NotebookCell): NotebookCell? {
    val indexInNote = cell.indexInNote
    val cells = cell.note?.cells ?: return null
    if (cells.size == indexInNote + 1)
      return null

    return cells[indexInNote + 1]
  }

  private fun notifyCellAction(event: AnActionEvent, body: (NoteEditorActionListener, NotebookCell) -> Unit) {
    val (project, cell, notebookVirtualFile) = getNotebookExecutionData(event) ?: return
    val notebookEditor = getNoteEditor(project, notebookVirtualFile)

    notebookEditor?.actionNotify {
      body(it, cell)
    }
  }

  private fun getNoteEditor(project: Project, notebookVirtualFile: NotebookVirtualFile) =
    FileEditorManager.getInstance(project).allEditors
      .asSequence()
      .mapNotNull { if (it is BdiDecoratableEditor) it.delegate else it }
      .filterIsInstance<NotebookEditor>()
      .firstOrNull { it.file == notebookVirtualFile }


  companion object {
    fun getNotebookExecutionData(event: AnActionEvent): NotebookExecutionData? {
      val editor = event.noteEditor ?: return null
      val project = editor.project ?: event.project ?: return null
      val notebookVirtualFile = editor.notebookVirtualFile ?: return null
      val cell = event.noteCell ?: return null
      return NotebookExecutionData(project, cell, notebookVirtualFile)
    }

    // When we are pressing hotkey in editor, we have no cell in context, and we need to find it by cursor position.
    val AnActionEvent.noteCell: NotebookCell?
      get() = getData(NotebookDataKeys.NOTE_CELL) ?: getNotebookCellByOffset(this)

    // When we are pressing hotkey in editor, we have no cell in context, and we need to find it by cursor position.
    val AnActionEvent.lineOffset: Int?
      get() = getData(NotebookDataKeys.SELECTED_LINE) ?: getOffsetOfCaretStartLine(this)

    // When pressing hotkey in Title or in output, we should use NOTE_EDITOR from context.
    val AnActionEvent.noteEditor: EditorImpl?
      get() = getData(NotebookDataKeys.NOTE_EDITOR) ?: getData(CommonDataKeys.EDITOR) as? EditorImpl
              ?: (getData(PlatformCoreDataKeys.FILE_EDITOR) as? NotebookEditor)?.editor as? EditorImpl

    val AnActionEvent.notebookVirtualFile: NotebookVirtualFile?
      get() = noteEditor?.notebookVirtualFile

    val EditorImpl.notebookVirtualFile: NotebookVirtualFile?
      get() = virtualFile as? NotebookVirtualFile
  }

  enum class RelativeDestination {
    ABOVE,
    BELOW
  }

  data class NotebookExecutionData(val project: Project, val cell: NotebookCell, val notebookVirtualFile: NotebookVirtualFile)
}