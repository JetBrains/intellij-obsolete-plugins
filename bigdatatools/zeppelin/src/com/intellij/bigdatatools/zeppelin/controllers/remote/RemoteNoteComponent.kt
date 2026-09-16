package com.intellij.bigdatatools.zeppelin.controllers.remote

import com.intellij.bigdatatools.coreUi.connection.exception.BdtConnectionException
import com.intellij.bigdatatools.notebooks.core.api.editor.NoteEditorActionListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.notebooks.core.impl.editor.external.ExternalNotebookModifier
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.service.ZeppelinNoteCacheConnection
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.bigdatatools.common.util.toPresentableText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Suppress("LeakingThis")
abstract class RemoteNoteComponent(val zeppelinEditor: ZeppelinEditor, val connection: ZeppelinNoteCacheConnection) : Disposable {
  protected val editor = zeppelinEditor.editor
  protected val project = zeppelinEditor.editor.project ?: error("Project is not found")
  protected val api get() = connection.api
  protected val note = zeppelinEditor.note
  protected val noteFile = zeppelinEditor.file
  protected val noteModifier = ExternalNotebookModifier(editor)
  protected abstract val connectionListener: ZeppelinConnectionListener
  protected abstract val actionListener: NoteEditorActionListener

  private var newCellWaitingIndex = -1
  private var newCellWaitingId: String? = null
  protected var newCellIdWaiter: CompletableDeferred<Unit>? = null
  private var newCellBodyWaiter: CompletableDeferred<Unit>? = null
  protected var waitingCell: ZeppelinCell? = null

  private val executionQueueManager = ExecutionQueueManagerComponent(this)

  //Service methods
  private val counterContext = newSingleThreadContext("NoteSynchronizationThread")
  private val singleScope = CoroutineScope(counterContext)
  private val toServerMutex = Mutex()

  init {
    Disposer.register(this, executionQueueManager)
  }

  override fun dispose() {
    counterContext.close()

    connection.removeListener(connectionListener)
    zeppelinEditor.removeActionListener(actionListener)
  }

  abstract fun executeCell(cell: ZeppelinCell)

  protected fun <T> runToServer(body: suspend () -> T) {
    singleScope.launch {
      toServerMutex.withLock {
        if (!connection.isConnected()) {
          notifySendToServerConnectionError()
          return@launch
        }
        try {
          body()
        }
        catch (t: BdtConnectionException) {
          logger.warn(t)
          invokeLater {
            if (!editor.isDisposed) {
              @Suppress("HardCodedStringLiteral") // Exception cannot be localized.
              HintManager.getInstance().showErrorHint(editor, t.toPresentableText())
            }
          }
        }
        catch (t: Throwable) {
          logger.error("Cannot perform to server command", t)
        }
      }
    }
  }

  protected fun runFromServer(body: suspend () -> Unit) {
    singleScope.launch {
      try {
        if (!editor.document.isWritable) {
          logger.error("Cannot update note from server, the document is read only")
          return@launch
        }

        body()
      }
      catch (t: Throwable) {
        logger.error("Cannot perform from server command", t)
      }
    }
  }

  protected fun updateIfWaitedCellId(index: Int, cell: ZeppelinCell): Boolean {
    if (newCellWaitingIndex == index) {
      val waitingCell = waitingCell ?: return false
      noteModifier.runTransaction {
        if (!waitingCell.isInNote) {
          newCellIdWaiter?.complete(Unit)
          newCellBodyWaiter?.complete(Unit)
          api.removeParagraph(cell.id)

          newCellWaitingIndex = -1
          newCellIdWaiter = null
          newCellBodyWaiter = null
          this.waitingCell = null
          return@runTransaction
        }

        waitingCell.id = cell.id
      }
      newCellWaitingId = cell.id
      newCellIdWaiter?.complete(Unit)
      return true
    }
    return false
  }

  protected fun updateIfWaitedCellBody(receivedCell: ZeppelinCell): Boolean {
    if (newCellWaitingId == receivedCell.id) {
      newCellBodyWaiter?.complete(Unit)
      return true
    }
    return false
  }

  protected abstract fun notifySendToServerConnectionError()

  protected suspend fun addAndWaitCellFromServer(cell: ZeppelinCell, index: Int): Unit = try {
    val cellIdWait = CompletableDeferred<Unit>()
    val cellBodyWait = CompletableDeferred<Unit>()
    newCellWaitingIndex = index
    newCellIdWaiter = cellIdWait
    newCellBodyWaiter = cellBodyWait
    waitingCell = cell

    log("Start add cell to server", cell)
    api.createParagraph(index, cell)

    log("Start wait id for new cell", cell)
    cellIdWait.await()

    log("Start wait body for new cell", cell)
    cellBodyWait.await()

    waitingCell = null
    newCellWaitingIndex = -1
    newCellWaitingId = null
    newCellIdWaiter = null
    newCellBodyWaiter = null

    log("Cell is added to server", cell)
  }
  catch (e: Error) {
    logger.error("Error on Add Cell", e)
    throw e
  }

  protected fun goBelow(cell: NotebookCell) = invokeAndWaitIfNeeded {
    val nextIndex = cell.indexInNote + 1
    if (nextIndex < note.cells.size) {
      val nextCell = note.cells[nextIndex]
      NotebookEditorUtils.goToCell(editor, nextCell)
    }
    else {
      NotebookEditorUtils.goToOffset(editor, editor.document.textLength)
    }
  }

  protected fun log(description: String, cell: ZeppelinCell) {
    if (logger.isTraceEnabled) {
      logger.trace("$description. Index: ${cell.indexInNote}, Id: ${cell.id}, Text: ${cell.text}")
    }
  }

  companion object {
    @JvmStatic
    protected val logger = Logger.getInstance(this::class.java)
  }
}