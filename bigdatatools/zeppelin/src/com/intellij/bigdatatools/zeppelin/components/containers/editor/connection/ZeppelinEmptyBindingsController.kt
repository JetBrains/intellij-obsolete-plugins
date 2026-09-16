package com.intellij.bigdatatools.zeppelin.components.containers.editor.connection

import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.controller.ZeppelinNoteController
import com.intellij.bigdatatools.zeppelin.models.interpreter.Interpreter
import com.intellij.openapi.Disposable
import com.intellij.ui.EditorNotifications

class ZeppelinEmptyBindingsController(val controller: ZeppelinNoteController) : Disposable {
  val connection = controller.cachedConnection
  val noteId = controller.noteId
  val configId = controller.config.innerId
  val listener = object : ZeppelinConnectionListener {
    override fun updateInterpreterBindings(bindings: List<Interpreter>) {
      controller.file.let {
        EditorNotifications.getInstance(controller.project).updateNotifications(it.originFile)
      }
    }
  }

  init {
    connection.addListener(listener)
    cachedNotifiers[configId to noteId] = this
  }

  override fun dispose() {
    connection.removeListener(listener)
    cachedNotifiers.remove(configId to noteId)
  }

  fun hasBindings(): Boolean {
    val bindings = connection.interpreterBindings
    return bindings.isEmpty() || bindings.any { it.selected }
  }

  companion object {
    val cachedNotifiers: MutableMap<Pair<String, String>, ZeppelinEmptyBindingsController> = mutableMapOf()
  }
}