package com.intellij.bigdatatools.notebooks.core.api.executor

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.openapi.editor.Editor


interface NoteExecutionListener {
  fun onProgress(editor: Editor, cell: NotebookCell, percentage: Int)
  fun onOutput(editor: Editor, cell: NotebookCell, data: String, index: Int, type: CellResultType, update: Boolean) {}
  fun onParagraphInfo(editor: Editor, cell: NotebookCell, info: Map<String, Any>) {}
}