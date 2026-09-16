package com.intellij.bigdatatools.notebooks.core.impl.controllers

import com.intellij.bigdatatools.notebooks.core.api.NotebookConstants
import com.intellij.bigdatatools.notebooks.core.impl.document.NoteDocumentFileUtil
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.jetbrains.bigdatatools.common.util.invokeLater

object NoteGuardBlockHandler {
  fun setupFor(editor: Editor) {
    editor.colorsScheme.setColor(EditorColors.READONLY_FRAGMENT_BACKGROUND_COLOR, null)

    val document = editor.document

    EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(document) {
      //If smth go wrong in move line handler ignore document cannot be applied
      NoteDocumentFileUtil.setIgnoreDocumentChange(document, false)

      val guardedBlock = it.guardedBlock
      NotebookEditorUtils.showReadOnlyHint(editor, guardedBlock)
    }

    editor.caretModel.allCarets.forEach {
      it.moveToVisualPosition(VisualPosition(1, 0))
    }

    editor.caretModel.addCaretListener(object : CaretListener {
      override fun caretPositionChanged(event: CaretEvent) = moveCaretFromFirstSymbol(event)
      override fun caretAdded(event: CaretEvent) = moveCaretFromFirstSymbol(event)
      override fun caretRemoved(event: CaretEvent) = moveCaretFromFirstSymbol(event)

      private fun moveCaretFromFirstSymbol(event: CaretEvent) {
        val caret: Caret = event.caret ?: return
        if (caret.visualPosition == VisualPosition(0, 0)) {
          invokeLater {
            if (editor.isDisposed)
              return@invokeLater

            if (caret.offset < NotebookConstants.PARAGRAPH_DELIMITER.length)
              caret.moveToOffset(NotebookConstants.PARAGRAPH_DELIMITER.length)
          }
        }
      }
    })
  }
}