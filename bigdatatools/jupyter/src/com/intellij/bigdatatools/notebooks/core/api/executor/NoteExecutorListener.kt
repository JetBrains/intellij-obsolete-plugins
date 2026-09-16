package com.intellij.bigdatatools.notebooks.core.api.executor

import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookOutput
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType

interface NoteExecutorListener {
  fun onConnect()
  fun onConnectionError(t: Throwable)

  fun onCellProgress(cell: NotebookCell, percentage: Int)
  fun onCellOutput(cell: NotebookCell, data: String, index: Int, type: CellResultType?, update: Boolean) {}
  fun onCellInfo(cell: NotebookCell, info: Map<String, Any>) {}
  fun onCellResult(cell: NotebookCell, status: CellStatus, output: NotebookOutput?)
}