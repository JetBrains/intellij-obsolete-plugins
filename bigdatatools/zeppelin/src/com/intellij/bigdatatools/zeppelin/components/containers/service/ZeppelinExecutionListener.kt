package com.intellij.bigdatatools.zeppelin.components.containers.service

import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.inlay.InlaysZeppelinExecutionListener
import com.intellij.bigdatatools.zeppelin.models.connection.Progress
import com.intellij.bigdatatools.zeppelin.models.notebook.ParagraphOutput
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.openapi.Disposable

class ZeppelinExecutionListener(val zeppelinEditor: ZeppelinEditor, val connection: ZeppelinNoteCacheConnection) : Disposable {
  private val listeners = listOf(InlaysZeppelinExecutionListener(), zeppelinEditor.executionProgressController)

  private val noteVirtualFile = zeppelinEditor.file
  private val note = noteVirtualFile.notebook as ZeppelinNotebook

  private val connectionListener = object : ZeppelinConnectionListener {
    override fun updateProgress(progress: Progress) {
      val cell = note.cells.firstOrNull { it.id == progress.id } ?: return
      listeners.forEach {
        it.onProgress(zeppelinEditor.editor, cell, percentage = progress.progress)
      }
    }

    override fun onParagraphInfo(info: Map<String, Any>) {
      val cell = note.cells.firstOrNull { it.id == info["id"] } ?: return
      listeners.forEach {
        it.onParagraphInfo(zeppelinEditor.editor, cell, info)
      }
    }

    override fun updateOutput(paragraphOutput: ParagraphOutput, isUpdate: Boolean) {
      if (connection.noteId != paragraphOutput.noteId)
        return

      val cell = note.cells.firstOrNull { it.id == paragraphOutput.paragraphId } ?: return
      listeners.forEach {
        it.onOutput(zeppelinEditor.editor, cell, paragraphOutput.data, paragraphOutput.index,
                    paragraphOutput.type ?: CellResultType.TEXT,
                    isUpdate)
      }
    }
  }

  init {
    connection.addListener(connectionListener)
  }

  override fun dispose() {
    connection.removeListener(connectionListener)
  }
}