package com.intellij.bigdatatools.zeppelin.interpreters.components

import com.intellij.bigdatatools.notebooks.core.api.editor.NoteEditorActionListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.zeppelin.components.containers.controller.ZeppelinNoteController
import com.intellij.bigdatatools.zeppelin.components.instance.ZeppelinConnectionManager
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinMarkerResolver
import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.common.util.invokeLater

class InterpretersActionsHandler(val zepEditor: ZeppelinEditor) : Disposable {
  val listener = object : NoteEditorActionListener {
    override fun restartInterpreter(cell: NotebookCell) {
      val project = zepEditor.project
      val noteCachedConnection = ZeppelinConnectionManager.getNoteConnectionByEditor(zepEditor) ?: return

      val bindings = noteCachedConnection.interpreterBindings

      val noteController = ZeppelinNoteController(project, noteCachedConnection, zepEditor)
      val interpreter = ZeppelinMarkerResolver.getInterpreterByMarker(cell, bindings) ?: return
      invokeLater {
        noteController.restartInterpreterWithConfirmation(interpreter)
      }
    }
  }

  init {
    zepEditor.addActionListener(listener)
  }

  override fun dispose() = zepEditor.removeActionListener(listener)
}