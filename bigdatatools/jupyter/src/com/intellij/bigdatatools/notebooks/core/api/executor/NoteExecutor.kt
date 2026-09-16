package com.intellij.bigdatatools.notebooks.core.api.executor

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.jetbrains.bigdatatools.common.rfs.driver.DriverConnectionStatus

interface NoteExecutor : Disposable {
  val presentableName: String
  val id: String
  val connectionStatus: DriverConnectionStatus

  override fun dispose() {
    if (connectionStatus.isConnected())
      disconnect()
  }

  fun connect()
  fun disconnect()

  fun getNoteExecutionListeners(): List<NoteExecutionListener>
  fun getNoteExecutorToolbarActions(): List<AnAction>

  fun runCell(cell: NotebookCell) {}
  fun stopCell(cell: NotebookCell) {}
  fun restartInterpreter(cell: NotebookCell) {}

  fun addListener(listener: NoteExecutorListener)
  fun removeListener(listener: NoteExecutorListener)
}