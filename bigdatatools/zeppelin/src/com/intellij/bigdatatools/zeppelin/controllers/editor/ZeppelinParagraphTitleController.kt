package com.intellij.bigdatatools.zeppelin.controllers.editor

import com.intellij.bigdatatools.notebooks.core.api.editor.NoteEditorActionListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.impl.EditorImpl
import com.jetbrains.bigdatatools.common.util.invokeLater

/**
 * Automatically adds paragraph titles as inlay - AfterLineEndElement.
 * Also listens for notebook events and switches title on and off according to settings.
 */
class ZeppelinParagraphTitleController(private val zeppelinEditor: ZeppelinEditor) : Disposable {
  val editor = zeppelinEditor.editor as EditorImpl
  val project = editor.project ?: error("No project specified")
  val note = zeppelinEditor.note

  private val actionListener = object : NoteEditorActionListener {
    override fun changeCellTitleIsVisible(cell: NotebookCell) = invokeLater {
      note.performModification {
        cell.titleVisible = !cell.titleVisible
      }
    }
  }

  init {
    zeppelinEditor.addActionListener(actionListener)
  }

  override fun dispose() {
    zeppelinEditor.removeActionListener(actionListener)
  }

  companion object {
    fun generateTitle(cell: NotebookCell): String = when {
      !cell.titleVisible || cell.title == null -> "${cell.indexInNote + 1}"
      cell.title.isNullOrEmpty() -> "${cell.indexInNote + 1}: Untitled"
      else -> (cell.indexInNote + 1).toString() + ": " + cell.title
    }
  }
}