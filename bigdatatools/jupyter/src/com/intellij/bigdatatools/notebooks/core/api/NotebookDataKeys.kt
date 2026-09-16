package com.intellij.bigdatatools.notebooks.core.api

import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.editor.impl.EditorImpl

object NotebookDataKeys {
  val NOTE_EDITOR = DataKey.create<EditorImpl>("NoteEditorId")
  val NOTE_CELL = DataKey.create<NotebookCell>("NoteCellId")
  val SELECTED_LINE = DataKey.create<Int>("SELECTED_LINE")
  val NOTE = DataKey.create<BasicNotebook>("NoteId")
}