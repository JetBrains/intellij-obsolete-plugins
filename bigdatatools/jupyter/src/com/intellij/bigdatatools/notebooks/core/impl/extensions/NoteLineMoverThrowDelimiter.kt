package com.intellij.bigdatatools.notebooks.core.impl.extensions

import com.intellij.bigdatatools.notebooks.core.api.NotebookConstants
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.controllers.NoteCellsDelimiterController
import com.intellij.bigdatatools.notebooks.core.impl.document.NoteCustomEventsApplier
import com.intellij.bigdatatools.notebooks.core.impl.document.NoteDocumentFileUtil
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.codeInsight.editorActions.moveUpDown.LineMover
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.util.DocumentUtil

class NoteLineMoverThrowDelimiter : LineMover() {
  private val customEventsApplier = NoteCustomEventsApplier()

  override fun checkAvailable(editor: Editor, file: PsiFile, info: MoveInfo, down: Boolean): Boolean {
    val isCorrect = file.viewProvider.virtualFile is NotebookVirtualFile && super.checkAvailable(editor, file, info, down)
    if (!isCorrect)
      return false

    val toMove = info.toMove
    val document = editor.document

    //prohibit move up from the begin of the first cell
    if (!down && (toMove.startLine <= 1 ||
                  toMove.startLine == 2 && getLineText(document, 1)?.startsWith(NotebookConstants.INTERPRETER_MARKER) == true))
      return info.prohibitMove()

    //Prohibit move line with delimiter
    (toMove.startLine until toMove.endLine).forEach {
      if (isDelimiterOrCellMarker(document, it)) {
        return info.prohibitMove()
      }
    }

    //Use this mover just in case of moving throw delimiter
    val nextLine = toMove.endLine
    val prevLine = toMove.startLine - 1
    //We need to check next+1 and next line because delimiter is greedy left so we cannot move by casual handler to the last line of the cell
    return when {
      down && (isDelimiterOrCellMarker(document, nextLine) || isDelimiterOrCellMarker(document, nextLine + 1)) -> true
      !down && (isDelimiterOrCellMarker(document, prevLine) || isDelimiterOrCellMarker(document, nextLine)) -> true
      else -> false
    }

  }

  override fun beforeMove(editor: Editor, info: MoveInfo, down: Boolean) {
    val document = editor.document

    NoteDocumentFileUtil.setIgnoreDocumentChange(document, true)
    val delimiterController = NoteCellsDelimiterController.getForEditor(editor) ?: error("Note Cell Delimiter controller is not found")
    delimiterController.removeBlocksForCells()

    customEventsApplier.beforeChange(document)
    val cell = delimiterController.note.getCellByOffset(document.getLineStartOffset(info.toMove.startLine)) ?: error("Cell is not found")
    val moveLineCount = info.toMove.endLine - info.toMove.startLine
    val cellTextLineCount = getCellTextLineCount(document, cell)

    if (cellTextLineCount == moveLineCount) {
      if (down) {
        document.insertString(cell.textOffset - 1, "\n")
        info.toMove = LineRange(info.toMove.startLine + 1, info.toMove.endLine + 1)
        info.toMove2 = LineRange(info.toMove2.startLine + 1, info.toMove2.endLine + 1)
      }
      else
        document.insertString(cell.textRange.endOffset, "\n")
    }
  }

  override fun afterMove(editor: Editor, file: PsiFile, info: MoveInfo, down: Boolean) {
    val document = editor.document
    NoteDocumentFileUtil.setIgnoreDocumentChange(document, false)
    NoteCellsDelimiterController.getForEditor(editor)?.refreshForAllCells()
    customEventsApplier.afterChange(document)
  }

  companion object {
    private fun getCellTextLineCount(document: Document, cell: NotebookCell): Int {
      val startLine = document.getLineNumber(cell.textOffset)
      val endLine = document.getLineNumber(cell.textRange.endOffset)
      return endLine - startLine
    }


    private fun isDelimiterOrCellMarker(document: Document, lineNumber: Int): Boolean {
      val lineText = getLineText(document, lineNumber) ?: return false
      return when {
        isDelimiter(lineText) -> true
        lineText.startsWith(NotebookConstants.INTERPRETER_MARKER) && isDelimiter(getLineText(document, lineNumber - 1)) -> true
        else -> false
      }

    }

    private fun isDelimiter(lineText: String?) = lineText == NotebookConstants.PARAGRAPH_DELIMITER.removeSuffix("\n")

    private fun getLineText(document: Document, lineNumber: Int): String? {
      if (lineNumber < 0 || lineNumber >= document.lineCount)
        return null
      val range = DocumentUtil.getLineTextRange(document, lineNumber)
      return document.getText(range)
    }
  }
}