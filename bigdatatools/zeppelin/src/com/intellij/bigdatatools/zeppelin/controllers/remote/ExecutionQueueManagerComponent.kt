package com.intellij.bigdatatools.zeppelin.controllers.remote

import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key

class ExecutionQueueManagerComponent(val remoteNote: RemoteNoteComponent) : Disposable {
  private val zeppelinEditor = remoteNote.zeppelinEditor
  private val project = zeppelinEditor.editor.project ?: error("Project is not found")

  private val connection = remoteNote.connection
  private val noteFile = zeppelinEditor.file
  private val note = zeppelinEditor.note
  val executionCellQueue = mutableListOf<ZeppelinCell>()

  private val notebookListener = object : NotebookChangeListener {
    override fun onEvent(notebookEvent: NotebookEvent) {
      if (notebookEvent !is CellChanged) return
      if (NotebookSchema.cellStatus !in notebookEvent.changedFields) return
      val cell = notebookEvent.cell as ZeppelinCell
      val status = cell.status

      val firstCellInQueue = executionCellQueue.firstOrNull() ?: return
      val isCellFromQueue = firstCellInQueue == cell

      when {
        status == CellStatus.ABORT || status == CellStatus.ERROR -> executionCellQueue.clear()
        status == CellStatus.FINISHED && isCellFromQueue -> {
          executionCellQueue.removeAt(0)
          val nextCell = executionCellQueue.firstOrNull() ?: return
          while (executionCellQueue.firstOrNull()?.isWithoutBody() == true) {
            executionCellQueue.removeAt(0)
          }

          executionCellQueue.firstOrNull()?.let {
            remoteNote.executeCell(nextCell)
          }
        }
      }
    }
  }


  private val connectionListener = object : ZeppelinConnectionListener {
    override fun onConnected() {
      executionCellQueue.clear()
    }

    override fun onDisconnected(statusCode: Int?, reason: String?) {
      executionCellQueue.clear()
    }
  }

  init {
    note.addNotebookChangeListener(notebookListener)
    connection.addListener(connectionListener)
    noteFile.putUserData(KEY, this)
  }

  override fun dispose() {
    askAboutTaskInQueue()
    note.removeNotebookChangeListener(notebookListener)
    connection.removeListener(connectionListener)
    noteFile.putUserData(KEY, null)
  }


  private fun hasTasksInQueue() = executionCellQueue.isNotEmpty()

  private fun runTasksFromQueueInBackground() {
    val tasks = executionCellQueue.toList()
    tasks.forEach {
      remoteNote.executeCell(it)
    }
  }

  private fun askAboutTaskInQueue() {
    if (!hasTasksInQueue()) return

    var result = 0
    invokeAndWaitIfNeeded {
      val title = ZepMessagesBundle.message("execution.queue.before.close.ask.title")
      val message = ZepMessagesBundle.message("execution.queue.before.close.ask.message")
      val runBackground = ZepMessagesBundle.message("execution.queue.before.close.ask.run.background")
      val clearQueue = ZepMessagesBundle.message("execution.queue.before.close.ask.clear.queue")
      result = Messages.showYesNoDialog(project, message, title, runBackground, clearQueue, Messages.getQuestionIcon())
    }

    if (result == Messages.YES) {
      runTasksFromQueueInBackground()
    }

    Thread.sleep(1000)
  }

  companion object {
    val KEY = Key<ExecutionQueueManagerComponent>("ZEPPELIN_EXECUTION_SERVICE")
  }
}