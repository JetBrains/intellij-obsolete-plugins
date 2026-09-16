package com.intellij.bigdatatools.zeppelin.statistics

import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.controller.ZeppelinNoteController
import com.intellij.bigdatatools.zeppelin.models.interpreter.Interpreter
import com.intellij.openapi.Disposable

class ZeppelinNoteStatisticListener(val controller: ZeppelinNoteController) : Disposable {
  private val cacheConnection = controller.cachedConnection

  private val listener = object : ZeppelinConnectionListener {
    override fun updateInterpreterBindings(bindings: List<Interpreter>) {
      ZeppelinBindingsUsageCollector.bindingsUpdate(cacheConnection.getContext(), bindings)
    }
  }

  init {
    cacheConnection.addListener(listener)
  }

  override fun dispose() {
    cacheConnection.removeListener(listener)
  }
}