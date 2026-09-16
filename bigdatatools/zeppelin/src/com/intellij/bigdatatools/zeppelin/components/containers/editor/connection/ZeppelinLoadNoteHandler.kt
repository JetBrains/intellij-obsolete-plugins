package com.intellij.bigdatatools.zeppelin.components.containers.editor.connection

import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.controller.ZeppelinNoteController
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinNoteLoadPromise
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable

class ZeppelinLoadNoteHandler(val controller: ZeppelinNoteController) : Disposable {
  val zeppelinEditor = controller.zeppelinEditor
  val connection = controller.cachedConnection

  val listener = object : ZeppelinConnectionListener {
    override fun updateNotebook(notebook: ZeppelinNotebook) {
      val error = if (notebook.cells.isEmpty())
        ZepMessagesBundle.message("filesystem.open.corrupted.note.message")
      else
        null
      ZeppelinNoteLoadPromise.makeLoaded(zeppelinEditor, error)
      connection.removeListener(this)
    }
  }

  init {
    connection.addListener(listener)
  }

  override fun dispose() {
    connection.removeListener(listener)
  }
}