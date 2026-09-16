package com.intellij.bigdatatools.notebooks.core.impl.controllers

import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.style.LinesNumberingMode
import com.intellij.bigdatatools.notebooks.style.NoteStyleSettings
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LineNumberConverter
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ex.EditorGutterComponentEx

/**
 * We have several line numbering models [LinesNumberingMode]. This class applies to given editor, selected
 * [NoteStyleSettings.linesNumbering].
 */
object NoteLineNumberingController {
  fun update(editor: Editor, notebook: BasicNotebook) {

    val converter = when (NoteStyleSettings.getInstance().linesNumbering) {
      LinesNumberingMode.CELL -> CellLineNumberConverter(notebook)
      LinesNumberingMode.DOCUMENT -> NoteLineNumberConverter(notebook)
    }

    (editor.gutter as EditorGutterComponentEx).setLineNumberConverter(converter)
  }

  class CellLineNumberConverter(private val notebook: BasicNotebook) : LineNumberConverter {
    override fun convert(editor: Editor, lineNumber: Int): Int {
      val offset = editor.logicalPositionToOffset(LogicalPosition(lineNumber - 1, 0))
      val cell = notebook.getCellByOffset(offset) ?: notebook.cells.last()
      val startLineOfInlay = editor.offsetToLogicalPosition(cell.textOffset).line
      return lineNumber - startLineOfInlay
    }

    override fun getMaxLineNumber(editor: Editor) = editor.document.lineCount
  }

  class NoteLineNumberConverter(private val notebook: BasicNotebook) : LineNumberConverter {
    override fun convert(editor: Editor, lineNumber: Int): Int {
      val offset = editor.logicalPositionToOffset(LogicalPosition(lineNumber - 1, 0))
      val cell = notebook.getCellByOffset(offset) ?: notebook.cells.last()
      return lineNumber - (cell.indexInNote + 1)
    }

    override fun getMaxLineNumber(editor: Editor) = editor.document.lineCount - notebook.cells.size
  }
}