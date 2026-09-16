package com.intellij.bigdatatools.notebooks.core.api.executor

import com.intellij.bigdatatools.notebooks.core.api.editor.NotebookEditor
import javax.swing.Icon

interface NoteExecutable {
  val presentableName: String
  val icon: Icon

  fun createExecutor(noteEditor: NotebookEditor): NoteExecutor

  fun getExternalId(): String
}