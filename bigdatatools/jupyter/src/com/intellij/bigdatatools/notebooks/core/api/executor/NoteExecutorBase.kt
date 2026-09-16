package com.intellij.bigdatatools.notebooks.core.api.executor

import com.intellij.openapi.diagnostic.Logger

abstract class NoteExecutorBase : NoteExecutor {
  private var listeners = mutableListOf<NoteExecutorListener>()

  fun notifyListeners(body: (NoteExecutorListener) -> Unit) {
    listeners.forEach {
      try {
        body(it)
      }
      catch (t: Throwable) {
        logger.error(t)
      }
    }
  }

  override fun addListener(listener: NoteExecutorListener) {
    listeners += listener
  }

  override fun removeListener(listener: NoteExecutorListener) {
    listeners -= listener
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}