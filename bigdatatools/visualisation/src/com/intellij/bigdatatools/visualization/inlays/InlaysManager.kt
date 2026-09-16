package com.intellij.bigdatatools.visualization.inlays

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor

interface InlaysManager {

  companion object {
    fun getInstance(): InlaysManager = service()
  }

  fun onNotebookOpened(editor: Editor)
  fun onProgress(editor: Editor, cell: NotebookCell, percentage: Int)
  fun onOutput(editor: Editor, cell: NotebookCell, data: String, update: Boolean, type: CellResultType = CellResultType.TEXT)
  fun onParagraphInfo(editor: Editor, cell: NotebookCell, info: Map<String, Any>)
}