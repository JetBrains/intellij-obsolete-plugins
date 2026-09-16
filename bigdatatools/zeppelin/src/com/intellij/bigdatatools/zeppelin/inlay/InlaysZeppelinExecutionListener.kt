package com.intellij.bigdatatools.zeppelin.inlay

import com.intellij.bigdatatools.notebooks.core.api.executor.NoteExecutionListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.bigdatatools.visualization.inlays.InlaysManager
import com.intellij.openapi.editor.Editor

class InlaysZeppelinExecutionListener : NoteExecutionListener {
  override fun onProgress(editor: Editor, cell: NotebookCell, percentage: Int) =
    InlaysManager.getInstance().onProgress(editor, cell, percentage)

  override fun onOutput(editor: Editor, cell: NotebookCell, data: String, index: Int, type: CellResultType, update: Boolean) =
    InlaysManager.getInstance().onOutput(editor, cell, data, update, type)

  override fun onParagraphInfo(editor: Editor, cell: NotebookCell, info: Map<String, Any>) {
    InlaysManager.getInstance().onParagraphInfo(editor, cell, info)
  }
}