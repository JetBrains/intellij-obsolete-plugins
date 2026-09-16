package com.intellij.bigdatatools.zeppelin.controllers.remote

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.notebooks.core.api.editor.NoteEditorActionListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellAdded
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellRemoved
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookCellUtil
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookUtils
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.service.ZeppelinNoteCacheConnection
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.models.connection.AngularRemoveResponse
import com.intellij.bigdatatools.zeppelin.models.connection.AngularUpdateResponse
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCellBuilder
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.utils.PatchUtils
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.util.Disposer
import com.intellij.util.Alarm
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class SynchronizeRemoteNoteComponent(zeppelinEditor: ZeppelinEditor, connection: ZeppelinNoteCacheConnection) :
  RemoteNoteComponent(zeppelinEditor, connection) {
  private val isInited = AtomicBoolean(false)
  private val remoteTexts = note.cells.associate { it.id to it.text }.toMutableMap()

  private val waitingChangingCells = ConcurrentLinkedQueue<ZeppelinCell>()
  private var moveCaretToCellIndex: Int? = null
  private var moveToCellId: String? = null

  private var dirtyCell: ZeppelinCell? = null

  private val deselectListener: (Unit) -> Unit = { commitUnsavedChanges() }

  private val updateCellAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

  override val connectionListener = object : ZeppelinConnectionListener {
    override fun onDisconnected(statusCode: Int?, reason: String?) {
      moveCaretToCellIndex = null
      moveToCellId = null
      waitingChangingCells.clear()
      updateCellAlarm.cancelAllRequests()
      newCellIdWaiter?.completeExceptionally(Exception("Disconnect. Status Code: $statusCode Reason: $reason "))
    }

    override fun updateNotebook(notebook: ZeppelinNotebook) {
      if (notebook.cells.isEmpty()) {
        return
      }

      if (notebook.cells.any { it.id.isBlank() }) {
        val indexes = notebook.cells.withIndex().filter { it.value.id.isBlank() }.joinToString { it.index.toString() }
        NotificationUtils.notifyText(ZepMessagesBundle.message("note.contains.empty.ids", notebook.clearName, indexes))
      }

      moveCaretToCellIndex = null
      moveToCellId = null

      remoteTexts.clear()
      remoteTexts.putAll(notebook.cells.map { it.id to it.text })

      noteFile.rename(null, notebook.clearName)
      noteFile.originFile.rename(null, notebook.clearName)

      val isUndoable = isInited.get()
      isInited.set(true)
      noteModifier.replaceNotebook(notebook, isUndoable)
    }

    override fun removeAngularObject(angularObject: AngularRemoveResponse) {
      if (note.id != angularObject.noteId)
        return
      noteModifier.runTransaction {
        note.removeAngularObject(angularObject)
      }
    }

    override fun updateAngularObject(newAngularObject: AngularUpdateResponse) {
      if (note.id != newAngularObject.noteId)
        return
      noteModifier.runTransaction {
        note.updateAngularObjects(newAngularObject)
      }
    }

    override fun addCell(jsonCell: JsonObject, index: Int) = runFromServer {
      val receivedCell = ZeppelinCell(note, jsonCell)
      remoteTexts[receivedCell.id] = receivedCell.text

      if (updateIfWaitedCellId(index, receivedCell))
        return@runFromServer

      log("Add cell from server", receivedCell)
      noteModifier.addCell(receivedCell, index)

      if (moveCaretToCellIndex == index)
        NotebookEditorUtils.goToCell(editor, note.cells[index])
      moveCaretToCellIndex = null
    }

    override fun removeCell(jsonCell: JsonObject) = runFromServer {
      val receivedCell = ZeppelinCell(note, jsonCell)

      if (!remoteTexts.containsKey(receivedCell.id)) {
        logger.warn("Remove unknown cell ${receivedCell.text}")
      }
      remoteTexts.remove(receivedCell.id)

      val cellInNote = getSimilarCellFromNote(receivedCell) ?: return@runFromServer
      log("Remove cell from server", receivedCell)
      cellInNote.let { noteModifier.removeCell(it) }
      if (dirtyCell?.id == cellInNote.id) {
        dirtyCell = null
      }
    }

    override fun moveCell(sourceId: String, toIndex: Int) = runFromServer {
      val cell = note.cells.firstOrNull { it.id == sourceId } ?: let {
        logger.warn("Move non-exists cell in note")
        return@runFromServer
      }
      if (cell.indexInNote == toIndex)
        return@runFromServer

      noteModifier.moveCell(cell, toIndex)
      if (cell.id == moveToCellId) {
        moveToCellId = null
        NotebookEditorUtils.goToCell(editor, cell)
      }
    }

    override fun updateCell(paragraphJson: JsonObject) = runFromServer {
      val receivedCell = ZeppelinCell(note, paragraphJson)
      remoteTexts[receivedCell.id] ?: let {
        if (waitingCell != null && receivedCell.text.isEmpty())
        //If you create cell from IDEA for Zeppelin 0.8 it can send change cell before add cell
        // we need ignore it
          return@runFromServer

        logger.warn("Update non-exists remote paragraph")
      }

      val isSelfChanged = removeFromChangeCell(receivedCell)
      if (isSelfChanged) {
        return@runFromServer
      }

      val cellInNote = getSimilarCellFromNote(receivedCell) ?: let {
        logger.warn("Update non-exists cell in note")
        return@runFromServer
      }
      remoteTexts[receivedCell.id] = receivedCell.text
      log("Update cell from server", receivedCell)

      if (updateIfWaitedCellBody(receivedCell)) {
        log("Update from server waiting new cell", receivedCell)
        return@runFromServer
      }
      noteModifier.updateCell(cellInNote, receivedCell)
      noteModifier.changeSyncStatus(cellInNote, true)
    }

    override fun pathParagraph(paragraphId: String, patch: String) {
      val remoteText = remoteTexts[paragraphId] ?: let {
        logger.error("Patch non-exists remote paragraph")
        ""
      }

      val newText = PatchUtils.applyPatch(remoteText, patch)
      remoteTexts[paragraphId] = newText

      val cell = note.cells.firstOrNull { it.id == paragraphId } ?: let {
        logger.error("Cell for patch is not found")
        return
      }
      val newSource = NotebookCellUtil.toSource(newText)

      val newCell = cell.copy()
      newCell.runWithMuteNotification {
        newCell.source = newSource
      }
      noteModifier.updateCell(cell, newCell)
      noteModifier.changeSyncStatus(cell, true)
    }

    private fun getSimilarCellFromNote(cell: ZeppelinCell) = note.cells.firstOrNull { it.id == cell.id }
  }

  override val actionListener = object : NoteEditorActionListener {
    override fun runAll() = runToServer {
      commitUnsavedChangesInner()
      api.runAll(note.id, note.cells)
    }

    override fun clearAllOutput() = runToServer {
      commitUnsavedChangesInner()
      api.clearAllOutputs(note.id)
    }

    override fun clearCellOutput(cell: NotebookCell) = runToServer {
      commitUnsavedChangesInner()
      api.clearParagraphOutput(cell as ZeppelinCell)
    }

    override fun stopAll() = runToServer {
      commitUnsavedChangesInner()

      api.stopAllParagraphs(note.id)
    }

    override fun runCell(cell: NotebookCell) = executeCell(cell as ZeppelinCell)

    override fun deleteCell(cell: NotebookCell) = runToServer {
      commitUnsavedChangesInner()
      api.removeParagraph(cell.id)
    }

    override fun addCell(cellIndex: Int, cellText: String) = runToServer {
      commitUnsavedChangesInner()
      val newCell = ZeppelinCellBuilder.createFromText(note, cellText)

      moveCaretToCellIndex = cellIndex

      api.createParagraph(cellIndex, newCell)
    }

    override fun addCell(cellIndex: Int,
                         cellText: String,
                         isTableHidden: Boolean,
                         isEditorHidden: Boolean,
                         metadata: Map<String, JsonElement>,
                         scrollToCell: Boolean) = runToServer {
      commitUnsavedChangesInner()

      val newCell = ZeppelinCellBuilder.createFromText(note, cellText)
      newCell.runWithMuteNotification {
        newCell.tableHide = isTableHidden
        newCell.editorHide = isEditorHidden
        for ((key, value) in metadata) {
          newCell.setMetadata(key, value)
        }
      }

      if (scrollToCell) moveCaretToCellIndex = cellIndex

      api.createParagraph(cellIndex, newCell)
    }

    override fun cloneCell(cell: NotebookCell, index: Int) = runToServer {
      commitUnsavedChangesInner()

      moveCaretToCellIndex = index
      api.createParagraph(index, cell as ZeppelinCell)
    }

    override fun stopCell(cell: NotebookCell) = runToServer {
      commitUnsavedChangesInner()
      api.stopParagraph(cell.id)
    }

    override fun splitCell(cell: NotebookCell, lineOffset: Int) = runToServer {
      NotebookUtils.splitCell(zeppelinEditor, cell, lineOffset)
    }

    override fun mergeWithNext(cell: NotebookCell) = runToServer {
      NotebookUtils.mergeCellWithNext(zeppelinEditor, cell)
    }

    override fun moveCell(cell: NotebookCell, toIndex: Int) = runToServer {
      commitUnsavedChangesInner()
      moveToCellId = cell.id
      api.moveParagraph(cell.id, toIndex)
    }

    override fun runAllBelow(cell: NotebookCell) = runToServer {
      commitUnsavedChanges()

      val executingCells = note.cells.subList(cell.indexInNote, note.cells.size)
      zeppelinEditor.actionNotify {
        it.addPlanningExecutingCellsFromLocalQueue(executingCells)
      }
      api.runAll(note.id, executingCells)
    }

    override fun runAllAbove(cell: NotebookCell) = runToServer {
      commitUnsavedChanges()

      if (cell.indexInNote == 0)
        return@runToServer
      val executingCells = note.cells.subList(0, cell.indexInNote)
      zeppelinEditor.actionNotify {
        it.addPlanningExecutingCellsFromLocalQueue(executingCells)
      }

      api.runAll(note.id, executingCells)
    }

    override fun runCellGoBelow(cell: NotebookCell) = runToServer {
      commitUnsavedChangesInner()
      executeCell(cell as ZeppelinCell)
      goBelow(cell)
    }
  }

  private val noteListener = object : NotebookChangeListener {
    override fun onEvent(notebookEvent: NotebookEvent) {
      when (notebookEvent) {

        is CellAdded -> {
          noteModifier.changeSyncStatus(notebookEvent.cell, false)
          runToServer {
            commitUnsavedChangesInner()
            sendAddCell(notebookEvent.cell as ZeppelinCell)
          }
        }
        is CellRemoved -> {
          runToServer {
            commitUnsavedChangesInner()
            sentRemoveCell(notebookEvent.cell as ZeppelinCell)
          }
        }
        is CellChanged -> {
          if (notebookEvent.changedFields == setOf(NotebookSchema.isSynced))
            return

          val cell = notebookEvent.cell as ZeppelinCell
          if (connection.isCollaborative && NotebookSchema.cellText in notebookEvent.changedFields) {
            sentPatchCell(cell)
          }

          val changedWithoutText = notebookEvent.changedFields - NotebookSchema.cellText - NotebookSchema.cellSource - NotebookSchema.isSynced
          if (!connection.isCollaborative || changedWithoutText.isEmpty()) {

            updateParagraph(cell)
          }
          else runToServer {
            commitUnsavedChangesInner()
            sendChange(cell)
          }
        }
      }
    }
  }

  init {
    NotebookFileUtil.setContainerId(noteFile, connection.noteId)

    Disposer.register(this, noteModifier)
    noteModifier.addNotebookChangeListener(noteListener)
    connection.addListener(connectionListener)
    zeppelinEditor.addActionListener(actionListener)

    zeppelinEditor.deselectNotifier += deselectListener

    connection.refreshConnectionAsync(false, project)
    if (connection.isConnected()) {
      connection.refreshNotebook()
    }
  }

  override fun dispose() {
    zeppelinEditor.deselectNotifier -= deselectListener
    updateCellAlarm.cancelAllRequests()
    commitUnsavedChanges()
    noteModifier.removeNotebookChangeListener(noteListener)
    super.dispose()
  }

  override fun executeCell(cell: ZeppelinCell) = runToServer {
    commitUnsavedChangesInner()
    if (cell.isWithoutBody())
      return@runToServer

    if (!cell.isLaunched)
      api.runCell(cell)
  }

  override fun notifySendToServerConnectionError() {
    logger.warn("Cannot sent operation to server, connection is lost")
    val errorMsg = ZepMessagesBundle.message("error.sent.to.server.notification")
    invokeLater {
      if (!editor.isDisposed)
        HintManager.getInstance().showInformationHint(editor, errorMsg)
    }
  }

  private fun commitUnsavedChanges() = runToServer {
    commitUnsavedChangesInner()
  }


  private fun commitUnsavedChangesInner() {
    dirtyCell?.let {
      sendChange(it)
      dirtyCell = null
    }
  }

  private fun sendChange(cell: ZeppelinCell) {
    val cellCopy = cell.copy()
    waitingChangingCells.add(cellCopy)
    log("Commit update cell to server", cell)
    api.commitParagraph(cellCopy)
  }

  private suspend fun sendAddCell(cell: ZeppelinCell) {
    val indexInNote = cell.indexInNote
    if (indexInNote < 0) {
      logger.info("Try to sent to sever non-exists cell")
      return
    }

    if (cell.id != "") {
      logger.error("Invoke add cell to server with non-empty id")
      return
    }

    try {
      addAndWaitCellFromServer(cell, indexInNote)
      noteModifier.changeSyncStatus(cell, true)
    }
    catch (t: Throwable) {
      logger.warn("Exception on create cell, cell will be removed", t)
      if (editor.document.isWritable)
        noteModifier.removeCell(cell)
    }
  }

  private fun sentRemoveCell(cell: ZeppelinCell) {
    val id = cell.id
    if (id.isBlank()) {
      logger.warn("Removed cell id is empty")
      return
    }
    log("Send remove cell", cell)
    api.removeParagraph(id)
  }

  private fun updateParagraph(cell: ZeppelinCell) {
    log("Invoke update paragraph from local.", cell)
    if (cell.id == "") {
      log("Ignore not added yet cell", cell)
      return
    }
    if (dirtyCell == cell) {
      log("Dirty cell the same, ignore send update", cell)
      return
    }
    updateCellAlarm.cancelAllRequests()
    commitUnsavedChanges()
    updateCellAlarm.addRequest(
      {
        try {
          commitUnsavedChanges()
        }
        catch (t: Throwable) {
          logger.warn("Update cell alarm exception", t)
        }
      }, 3000)

    noteModifier.changeSyncStatus(cell, false)
    dirtyCell = cell

  }

  private fun sentPatchCell(cell: ZeppelinCell) = runToServer {
    val cellId = cell.id
    val oldText = remoteTexts[cellId] ?: let {
      logger.warn("Patched cell is not found")
      return@runToServer
    }
    val newText = cell.text

    val patch = PatchUtils.getPatch(oldText, newText)
    api.patchParagraph(cell.note.id, cellId, patch)
    remoteTexts[cellId] = newText
  }

  private fun removeFromChangeCell(paragraph: ZeppelinCell): Boolean {
    val changingCell = waitingChangingCells.firstOrNull {
      it.id == paragraph.id && it.text == paragraph.text
    } ?: return false

    waitingChangingCells.remove(changingCell)

    val cellInNote = note.cells.firstOrNull { it.id == paragraph.id }

    cellInNote?.let { noteModifier.changeSyncStatus(it, true) }

    return true
  }
}