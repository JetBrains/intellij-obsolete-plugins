package com.intellij.bigdatatools.zeppelin.controllers.remote

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.bigdatatools.notebooks.core.api.editor.NoteEditorActionListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookOutput
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultMessage
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.OutputCode
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.service.ZeppelinNoteCacheConnection
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.executor.ZeppelinNoteExecutor
import com.intellij.bigdatatools.zeppelin.models.connection.AngularRemoveResponse
import com.intellij.bigdatatools.zeppelin.models.connection.AngularUpdateResponse
import com.intellij.bigdatatools.zeppelin.models.connection.Progress
import com.intellij.bigdatatools.zeppelin.models.notebook.ParagraphOutput
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.bigdatatools.common.util.toPresentableText
import java.util.UUID
import java.util.concurrent.ConcurrentSkipListMap

class ZeppelinLocalNoteServerManager(zeppelinEditor: ZeppelinEditor,
                                     connection: ZeppelinNoteCacheConnection,
                                     val noteExecutor: ZeppelinNoteExecutor) : RemoteNoteComponent(zeppelinEditor,
                                                                                                   connection) {
  private val waitingCellsForRun = ConcurrentSkipListMap<String, NotebookCell>()
  private val remoteIdToCell = ConcurrentSkipListMap<String, NotebookCell>()

  override val connectionListener = object : ZeppelinConnectionListener {
    override fun onConnected() {
      noteExecutor.notifyListeners { it.onConnect() }
    }

    override fun onDisconnected(statusCode: Int?, reason: String?) {
      newCellIdWaiter?.completeExceptionally(Exception("Disconnect. Status Code: $statusCode Reason: $reason "))

      noteExecutor.notifyListeners {
        it.onConnectionError(Exception("Disconnected from Zeppelin server, status: $statusCode, reason: $reason"))
      }
    }

    override fun updateCell(paragraphJson: JsonObject) = runFromServer {
      val remoteCell = ZeppelinCell(note, paragraphJson)

      if (remoteCell.status == CellStatus.READY) {
        val startRunTaskId = (remoteCell.getMetadata(LOCAL_CELL_RUN_ID) as? JsonPrimitive)?.asString ?: return@runFromServer
        val localCell = waitingCellsForRun[startRunTaskId] ?: return@runFromServer
        remoteIdToCell[remoteCell.id] = localCell
        waitingCellsForRun.remove(startRunTaskId)

        innerRunCell(remoteCell, localCell)
      }

      val notebookCell = remoteIdToCell[remoteCell.id] ?: return@runFromServer
      val status = remoteCell.status

      if (remoteCell.status == CellStatus.READY)
        return@runFromServer

      noteExecutor.notifyListeners {
        it.onCellResult(notebookCell, status, remoteCell.output)
      }
    }

    override fun updateProgress(progress: Progress) {
      val notebookCell = remoteIdToCell[progress.id] ?: let {
        logger.warn("Cannot find cell for ${progress.id} to update progress")
        return
      }
      noteExecutor.notifyListeners {
        it.onCellProgress(notebookCell, progress.progress)
      }
    }

    override fun onParagraphInfo(info: Map<String, Any>) {
      val notebookCell = remoteIdToCell[info["id"]] ?: let {
        logger.warn("Cannot find cell for ${info["id"]} to update paragraph info")
        return
      }
      noteExecutor.notifyListeners {
        it.onCellInfo(notebookCell, info)
      }
    }

    override fun updateOutput(paragraphOutput: ParagraphOutput, isUpdate: Boolean) {
      if (connection.noteId != paragraphOutput.noteId)
        return

      val paragraphId = paragraphOutput.paragraphId
      val notebookCell = remoteIdToCell[paragraphId] ?: let {
        logger.warn("Cannot find cell for ${paragraphId} to update paragraph output")
        return
      }
      if (notebookCell.status.isFinished)
        return

      noteExecutor.notifyListeners {
        it.onCellOutput(notebookCell, paragraphOutput.data, paragraphOutput.index, paragraphOutput.type, isUpdate)
      }
    }

    override fun removeAngularObject(angularObject: AngularRemoveResponse) {
      noteModifier.runTransaction {
        note.removeAngularObject(angularObject)
      }
    }

    override fun updateAngularObject(newAngularObject: AngularUpdateResponse) {
      noteModifier.runTransaction {
        note.updateAngularObjects(newAngularObject)
      }
    }
  }

  override val actionListener = object : NoteEditorActionListener {}

  init {
    Disposer.register(this, noteModifier)
    connection.addListener(connectionListener)
    zeppelinEditor.addActionListener(actionListener)
  }

  override fun executeCell(cell: ZeppelinCell) = runToServer {
    try {
      api.clearTempNotebook()

      val remoteCell = cell.copy()
      val runTaskId = UUID.randomUUID().toString()
      remoteCell.performMutedChange {
        remoteCell.setMetadata(LOCAL_CELL_RUN_ID, JsonPrimitive(runTaskId))
      }

      waitingCellsForRun[runTaskId] = cell
      api.createParagraph(0, remoteCell)
    }
    catch (t: Throwable) {
      notifyExecutionCellError(cell, t)
      throw t
    }
  }

  fun stopCell(cell: ZeppelinCell) = runToServer {
    val cellId = remoteIdToCell.toList().firstOrNull {
      it.second == cell
    }?.first?.ifBlank { null } ?: return@runToServer
    if (cellId.isBlank())
      return@runToServer

    api.stopParagraph(cellId)
  }

  private fun innerRunCell(remoteCell: ZeppelinCell, localCell: NotebookCell) = runToServer {
    try {
      api.runCell(remoteCell)
    }
    catch (t: Throwable) {
      notifyExecutionCellError(localCell, t)
    }
  }

  private fun notifyExecutionCellError(cell: NotebookCell, t: Throwable) {
    noteExecutor.notifyListeners {
      it.onCellResult(cell, CellStatus.ERROR, NotebookOutput(OutputCode.ERROR, listOf(
        CellResultMessage(type = CellResultType.TEXT, data = t.toPresentableText()))))
    }
  }

  override fun notifySendToServerConnectionError() {
    logger.warn("Cannot sent operation to server, connection is lost")
    val errorMsg = ZepMessagesBundle.message("error.sent.to.server.notification")
    val errorTitle = ZepMessagesBundle.message("connection.error")
    invokeLater {
      if (!editor.isDisposed)
        Messages.showErrorDialog(project, errorMsg, errorTitle)
    }
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)

    private const val LOCAL_CELL_RUN_ID = "LOCAL_CELL_RUN_ID"
  }
}