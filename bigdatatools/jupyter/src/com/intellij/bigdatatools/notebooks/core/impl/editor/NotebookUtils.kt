package com.intellij.bigdatatools.notebooks.core.impl.editor

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.notebooks.core.api.editor.NotebookEditor
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionService.Companion.noteEditor
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionService.Companion.notebookVirtualFile
import com.intellij.bigdatatools.notebooks.core.impl.editor.external.ExternalNotebookModifier
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.NotebookCellBase
import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookPsiFile
import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiDocumentManagerBase
import com.jetbrains.bigdatatools.common.editor.BdiDecoratableEditor
import com.jetbrains.bigdatatools.common.util.invokeLater

fun VirtualFile.getNotebookPsiFileFile(project: Project) = PsiManager.getInstance(project).findFile(this) as NotebookPsiFile

fun Editor.getPsiFile(): PsiFile? {
  val project = project ?: return null
  val editorImpl = this as? EditorImpl ?: return null
  return PsiDocumentManager.getInstance(project).getPsiFile(editorImpl.document)
}

internal fun getOffsetOfCaretStartLine(actionEvent: AnActionEvent): Int? {
  val editor = actionEvent.noteEditor ?: return null
  return editor.caretModel.primaryCaret.visualLineStart
}

internal fun getNotebookCellByOffset(actionEvent: AnActionEvent): NotebookCell? {
  val editor = actionEvent.noteEditor ?: return null
  val notebookFile = editor.notebookVirtualFile ?: return null
  val cells = notebookFile.notebook.cells
  val activeCellIndex = getCellIndex(cells, editor.caretModel.primaryCaret.offset)
  return if (activeCellIndex < 0) null else cells[activeCellIndex]
}

internal fun getCellIndex(cells: List<NotebookCell>, offset: Int) = cells.indexOfLast { it.offset <= offset }

object NotebookEditorUtils {
  val READONLY_MARKER_REASON: Key<String> = Key("READONLY_MARKER_REASON")

  fun getNotebookEditor(project: Project, notebookVirtualFile: VirtualFile): NotebookEditor? {
    val notebookEditor = FileEditorManager.getInstance(project).getSelectedEditor(notebookVirtualFile) ?: return null
    return notebookEditor as? NotebookEditor ?: (notebookEditor as? BdiDecoratableEditor)?.delegate as? NotebookEditor
  }

  fun showReadOnlyHint(editor: Editor, guardedBlock: RangeMarker) {
    val reason = guardedBlock.getUserData(READONLY_MARKER_REASON) ?: return
    if (reason.isBlank())
      return

    HintManager.getInstance().showInformationHint(editor, NoteMessagesBundle.message("hint.readonly", reason), HintManager.UNDER)
  }

  fun goToCell(editor: Editor, cell: NotebookCell) {
    val interpreterLength = cell.interpreterCode.length
    val sourceOffset = cell.textOffset + (if (interpreterLength != 0) interpreterLength + 2 else 0)

    val correctOffset = if (sourceOffset >= editor.document.textLength)
      editor.document.textLength
    else
      sourceOffset.coerceAtMost(cell.textRange.endOffset - 1)

    goToOffset(editor, correctOffset)
  }

  fun goToOffset(editor: Editor, startOffset: Int) = invokeAndWaitIfNeeded {
    if (editor.isDisposed)
      return@invokeAndWaitIfNeeded

    val processor = CommandProcessor.getInstance()
    processor.executeCommand(editor.project, {
      editor.caretModel.removeSecondaryCarets()
      editor.caretModel.moveToOffset(startOffset)
      editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
      editor.selectionModel.removeSelection()
      IdeFocusManager.getGlobalInstance().requestFocus(editor.contentComponent, true)
      IdeDocumentHistory.getInstance(editor.project).includeCurrentCommandAsNavigation()
    }, NoteMessagesBundle.message("command.go.to.paragraph"), null)
  }

  fun getCellRangeInEditor(editor: Editor, cell: NotebookCell): TextRange {
    val cellRange = cell.textRange

    val documentLength = editor.document.textLength
    return if (cellRange.endOffset >= documentLength - 1)
      TextRange(cell.textOffset, documentLength)
    else
      TextRange(cell.textOffset, (cellRange.endOffset - 1).coerceAtLeast(cell.textOffset).coerceAtMost(documentLength))
  }

  fun refreshHighlight(notebookEditor: NotebookEditor) = invokeLater {
    val project = notebookEditor.editor.project ?: return@invokeLater
    val file = notebookEditor.file

    StartupManager.getInstance(project).runAfterOpened {
      invokeLater {
        PsiDocumentManager.getInstance(project).reparseFiles(listOf(file), false)
      }
    }
  }

  fun getPsiCells(project: Project, notebookVirtualFile: NotebookVirtualFile) = notebookVirtualFile.getNotebookPsiFileFile(project).cells

  fun handleError(project: Project, notebookFile: NotebookVirtualFile, t: Throwable) {
    FileEditorManager.getInstance(project).closeFile(notebookFile)
    FileEditorManager.getInstance(project).closeFile(notebookFile.originFile)
    val desc = NoteMessagesBundle.message("process.change.error.message")
    invokeLater {
      Messages.showErrorDialog(desc, NoteMessagesBundle.message("process.change.error.title"))
      NotificationUtils.notifyException(t, NoteMessagesBundle.message("process.change.error.bubble.title"))
    }
  }
}

object NotebookUtils {
  fun mergeCellWithNext(notebookEditor: NotebookEditor, cell: NotebookCell) {
    val notebookFile = notebookEditor.file
    val notebook = notebookFile.notebook

    if (cell.indexInNote >= notebook.cells.size - 1)
      return

    val nextCell = notebook.cells[cell.indexInNote + 1]
    val nextCellText = nextCell.text.removePrefix(nextCell.marker)

    val newText = cell.text + "\n" + nextCellText
    val newSource = NotebookCellUtil.toSource(newText)

    val noteModifier = ExternalNotebookModifier(notebookEditor.editor)

    val newCell = cell.copy() as NotebookCellBase
    newCell.runWithMuteNotification {
      newCell.source = newSource
    }
    noteModifier.updateCell(cell, newCell)
    Disposer.dispose(noteModifier)

    notebookEditor.actionNotify {
      it.deleteCell(nextCell)
    }
  }

  fun splitCell(notebookEditor: NotebookEditor, cell: NotebookCell, lineOffset: Int) {
    val editor = notebookEditor.editor
    val cellRange = cell.textRange
    if (!cellRange.containsOffset(lineOffset))
      return

    val firstCellSource = editor.document.getText(TextRange(cellRange.startOffset, lineOffset))
    val secondPart = editor.document.getText(TextRange(lineOffset, cellRange.endOffset))
    val cellMarker = cell.marker
    val secondCellText = (if (cellMarker.isBlank()) "" else (cellMarker + "\n")) + secondPart

    val noteModifier = ExternalNotebookModifier(editor)

    val newCell = cell.copy() as NotebookCellBase
    newCell.runWithMuteNotification {
      newCell.source = firstCellSource
    }
    noteModifier.updateCell(cell, newCell)
    Disposer.dispose(noteModifier)

    notebookEditor.actionNotify {
      it.addCell(cell.indexInNote + 1, secondCellText)
    }
  }
}

