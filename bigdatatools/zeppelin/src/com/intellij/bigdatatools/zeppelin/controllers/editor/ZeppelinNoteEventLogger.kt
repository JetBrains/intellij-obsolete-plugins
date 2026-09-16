package com.intellij.bigdatatools.zeppelin.controllers.editor

import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellAdded
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellRemoved
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger

class ZeppelinNoteEventLogger(private val note: ZeppelinNotebook) : Disposable {
  private val listener = object : NotebookChangeListener {
    override fun onEvent(notebookEvent: NotebookEvent) {
      when (notebookEvent) {
        is CellAdded -> {
          val cell = notebookEvent.cell as ZeppelinCell
          val index = notebookEvent.index
          log("Cell is added", cell, index)
        }
        is CellChanged -> {
          val cell = notebookEvent.cell as ZeppelinCell
          log("Cell is changed", cell, cell.indexInNote)
        }
        is CellRemoved -> {
          val cell = notebookEvent.cell as ZeppelinCell
          val index = notebookEvent.index
          log("Cell is removed", cell, index)
        }
      }
    }
  }

  init {
    note.addNotebookChangeListener(listener)
  }

  private fun log(description: String, cell: ZeppelinCell, index: Int) {
    val info = "Id: ${cell.id}, Index: $index, text: ${cell.text}, Note: ${cell.note.name}"
    if (logger.isTraceEnabled) {
      logger.trace("NoteEvent: $description. $info")
    }
  }

  override fun dispose() {
    note.removeNotebookChangeListener(listener)
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}