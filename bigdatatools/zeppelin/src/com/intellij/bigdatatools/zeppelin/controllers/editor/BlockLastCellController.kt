package com.intellij.bigdatatools.zeppelin.controllers.editor

import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellAdded
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellRemoved
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.connections.parser.ZeppelinVersionAdapter
import com.intellij.bigdatatools.zeppelin.components.containers.service.ZeppelinNoteCacheConnection
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.impl.EditorImpl
import java.util.concurrent.atomic.AtomicBoolean

class BlockLastCellController(zeppelinEditor: ZeppelinEditor,
                              private val connection: ZeppelinNoteCacheConnection) : Disposable {
  private val isEnabled = AtomicBoolean()
  private val editor = zeppelinEditor.editor as EditorImpl
  private val note = zeppelinEditor.note
  private var readOnlyMarker: RangeMarker? = null

  private val notebookChangeListener = object : NotebookChangeListener {
    private var isRequireUpdate = false

    override fun onEvent(notebookEvent: NotebookEvent) {
      if (!isEnabled.get()) return

      invokeAndWaitIfNeeded {
        val e = notebookEvent as? NotebookCellEvent ?: return@invokeAndWaitIfNeeded

        if (e is CellAdded || e is CellRemoved)
          isRequireUpdate = true

        if (e.isLastInBatch) {
          updateLastCellBlock()
        }
      }
    }
  }

  private val connectionListener = object : ZeppelinConnectionListener {
    override fun onConnected() {
      initBlock()
    }

    override fun onDisconnected(statusCode: Int?, reason: String?) {
      if (!isEnabled.get()) {
        return
      }
      invokeAndWaitIfNeeded {
        clear()
      }
    }
  }

  init {
    note.addNotebookChangeListener(notebookChangeListener)
    connection.addListener(connectionListener)

    initBlock()
  }

  override fun dispose() {
    note.removeNotebookChangeListener(notebookChangeListener)
    connection.removeListener(connectionListener)

    clear()
  }

  private fun initBlock() {
    isEnabled.set(false)
    clear()

    if (!connection.isConnected())
      return
    val info = connection.zeppelinInfo ?: return

    if (ZeppelinVersionAdapter.getInstance(info).isAllowedRemoveLastCell)
      return

    isEnabled.set(true)
    invokeAndWaitIfNeeded {
      updateLastCellBlock()
    }
  }


  private fun updateLastCellBlock() {
    clear()

    if (note.cells.size == 1)
      return

    val offset = note.cells.last().textRange.startOffset
    val newReadOnlyRange = editor.document.createGuardedBlock(offset, offset + 1)
    newReadOnlyRange.putUserData(NotebookEditorUtils.READONLY_MARKER_REASON,
                                 ZepMessagesBundle.message("notification.last.paragraph.no.edit"))
    newReadOnlyRange.isGreedyToLeft = true
    readOnlyMarker = newReadOnlyRange
  }

  private fun clear() {
    readOnlyMarker?.let {
      editor.document.removeGuardedBlock(it)
      readOnlyMarker = null
    }
  }
}