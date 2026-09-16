package com.intellij.bigdatatools.notebooks.core.impl.editor.external

import com.intellij.bigdatatools.notebooks.core.api.NotebookConstants
import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.controllers.NoteCellsDelimiterController
import com.intellij.bigdatatools.notebooks.core.impl.document.NoteDocumentFileUtil
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessor.commitDocument
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil

/**
 * Represent functions that provide way to modify Notebook Document from external
 */
internal class NotebookDocumentModifier(private val editor: Editor) {
  private val document = editor.document
  private val project = editor.project ?: throw Exception("Project is not found")
  private val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) as? NotebookVirtualFile
                            ?: throw Exception("Notebook Virtual File is not found")
  private val notebook = virtualFile.notebook

  fun replaceNotebook(newNotebook: BasicNotebook, isUndoable: Boolean) {
    if (newNotebook.asSource() != notebook.asSource())
      updateDocumentText(newNotebook.asSource(), isUndoable)
    notebook.replace(newNotebook)
  }

  fun addCell(cell: NotebookCell, cellIndex: Int) {
    if (cell.text.startsWith(NotebookConstants.PARAGRAPH_DELIMITER))
      error("Cannot add cell, which start by ${NotebookConstants.PARAGRAPH_DELIMITER}")
    addCellToDocument(cellIndex, cell.source)
    notebook.addCell(cell, cellIndex)
  }

  fun removeCell(index: Int) {
    //The order is important!
    val cells = notebook.cells
    if (cells.size == 1) {
      throw Exception("Cannot remove the 1 paragraph.")
    }
    val cell = cells[index]

    val delimiterBlockController = NoteCellsDelimiterController.getForEditor(editor)
                                   ?: error("Cannot find note cell delimiter controller")
    delimiterBlockController.withCellUnlock(cell) {
      removeCellFromDocument(index)
      notebook.removeCell(index)
    }
  }

  fun updateCell(newCell: NotebookCell, index: Int) {
    val oldCell = notebook.cells[index]
    val oldSource = oldCell.source
    val newSource = newCell.source

    if (newSource != oldSource)
      updateDocumentCellText(index, newSource)

    notebook.updateCell(newCell, index)
  }

  fun moveCell(cell: NotebookCell, toIndex: Int) {
    removeCell(cell.indexInNote)
    addCell(cell, toIndex)
  }

  fun clearCell(index: Int) {
    val notebookCell = notebook.cells[index]
    val delimiterBlockController = NoteCellsDelimiterController.getForEditor(editor)
                                   ?: error("Cannot find note cell delimiter controller")
    delimiterBlockController.withCellUnlock(notebookCell) {
      if (isLocked(index)) {
        throw CellLockedException(notebook.cells[index], index)
      }

      updateDocumentCellText(index, NotebookConstants.PARAGRAPH_DELIMITER)
    }
    notebookCell.clear()
  }

  private fun isLocked(index: Int): Boolean {
    val cells = notebook.cells
    val cell = cells[index]
    val textRange = cell.textRange
    val startOffset = textRange.startOffset + 1
    val rangeGuard = document.getRangeGuard(startOffset, textRange.endOffset - 1)
    return rangeGuard != null
  }

  private fun updateDocumentText(newText: String, isUndoable: Boolean) {
    executeWriteCommand(project) {
      if (!isUndoable) {
        val reference = DocumentReferenceManager.getInstance().create(document)
        UndoManager.getInstance(project).nonundoableActionPerformed(reference, false)
      }
      NoteDocumentFileUtil.setIgnoreDocumentChange(document, true)
      document.setText(newText)
    }
    commitDocument(editor)
    NoteDocumentFileUtil.setIgnoreDocumentChange(document, false)
  }

  private fun removeCellFromDocument(index: Int) {
    val cells = notebook.cells
    if (cells.size == 1) {
      throw Exception("Cannot remove the 1 paragraph.")
    }
    val cell = cells[index]
    runUndoTransparentWriteAction {
      moveCaretToNextCellIfRequired(cells, index)
      editor.selectionModel.removeSelection()
    }
    editor.selectionModel.removeSelection()
    if (isLocked(index)) {
      throw CellLockedException(notebook.cells[index], index)
    }

    executeWriteCommand(project) {
      NoteDocumentFileUtil.setIgnoreDocumentChange(document, true)
      val textRange = cell.textRange
      val cellOffset = textRange.startOffset

      document.replaceString(textRange.startOffset, textRange.endOffset, "")
      commitDocument(editor)
      NoteDocumentFileUtil.setIgnoreDocumentChange(document, false)
      if (cells.lastIndex == index) {
        removeLastLine(TextRange.create(cellOffset - 1, cellOffset))
      }
    }
  }

  /**
   * This fix bug that caret can be on zero offset if we remove the first cell
   */
  private fun moveCaretToNextCellIfRequired(cells: List<NotebookCell>, index: Int) {
    val cell = cells[index]
    if (!cell.textRange.contains(editor.caretModel.offset))
      return
    val offset = if (index < cells.lastIndex)
      cells[index + 1].textRange.startOffset + 1
    else
      document.textLength - 1

    editor.caretModel.moveToOffset(offset)
  }

  private fun updateDocumentCellText(index: Int, newSource: String) {
    val cells = notebook.cells

    val cell = cells[index]

    val replaced = if (index == cells.lastIndex) {
      newSource
    }
    else {
      newSource + "\n"
    }

    replaceCellTextInDocument(cell, replaced)
  }

  private fun addCellToDocument(cellIndex: Int, newCellText: String) {
    val newCellStartOffset = if (cellIndex == 0)
      0
    else
      notebook.cells[cellIndex - 1].textRange.endOffset
    val newCellSource = when {
      cellIndex != notebook.cells.size -> "$newCellText\n"
      else -> "\n$newCellText"
    }

    editor.selectionModel.removeSelection()
    executeWriteCommand(project) {
      NoteDocumentFileUtil.setIgnoreDocumentChange(document, true)
      document.insertString(newCellStartOffset, newCellSource)
      commitDocument(editor)
      NoteDocumentFileUtil.setIgnoreDocumentChange(document, false)
    }
  }

  /**
   * Update cell text in document
   * @param cell - a cell for update,
   * @param newText - new text for the cell
   */
  private fun replaceCellTextInDocument(cell: NotebookCell,
                                        newText: String) {
    val range = cell.textRange
    val delimiterBlockController = NoteCellsDelimiterController.getForEditor(editor)
                                   ?: error("Cannot find note cell delimiter controller")
    delimiterBlockController.withCellUnlock(cell) {
      replaceRangeInDocument(range, newText)
    }
  }

  private fun replaceRangeInDocument(range: TextRange, newText: String) {
    NoteDocumentFileUtil.setIgnoreDocumentChange(document, true)
    try {
      val withNewSeparators = StringUtil.convertLineSeparators(newText)
      executeWriteCommand(project) {
        editor.document.replaceString(range.startOffset, range.endOffset, withNewSeparators)
      }
      commitDocument(editor)
    }
    finally {
      NoteDocumentFileUtil.setIgnoreDocumentChange(document, false)
    }
  }

  private fun removeLastLine(range: TextRange) {
    NoteDocumentFileUtil.setIgnoreDocumentChange(document, true)
    editor.document.replaceString(range.startOffset, range.endOffset, "")
    commitDocument(editor)
    NoteDocumentFileUtil.setIgnoreDocumentChange(document, false)
  }

  private fun executeWriteCommand(project: Project, body: () -> Unit) {
    val runnable = Runnable {
      runWriteAction {
        body()
      }
    }
    CommandProcessor.getInstance().executeCommand(project, runnable, "", "")
  }
}