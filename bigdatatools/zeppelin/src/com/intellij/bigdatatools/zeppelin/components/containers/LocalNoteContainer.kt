package com.intellij.bigdatatools.zeppelin.components.containers

import com.intellij.bigdatatools.notebooks.core.api.executor.LocalNoteExecuteController
import com.intellij.bigdatatools.notebooks.core.api.executor.NoteExecutable
import com.intellij.bigdatatools.notebooks.core.api.executor.NoteExecutor
import com.intellij.bigdatatools.notebooks.core.api.executor.NoteExecutorUtils
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.components.containers.service.LocalNoteActionHandler
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinNoteLoadPromise
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.editor.actions.ExecutorManagementAction
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.statistics.ZeppelinNotebookUsageCollector
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ex.ToolbarLabelAction
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.jetbrains.bigdatatools.common.rfs.driver.ConnectedConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.ConnectingConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.FailedConnectionStatus
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.jetbrains.bigdatatools.common.util.toPresentableText

/**
 * A class, which represent the opened local notebook in IDEA
 */
class LocalNoteContainer(private val notebookEditor: ZeppelinEditor) : Disposable {
  val project: Project = notebookEditor.editor.project ?: error("Project is null")

  private val noteVirtualFile = notebookEditor.file
  private val notebook = notebookEditor.note

  val executorsProvider: List<NoteExecutable>
    get() = NoteExecutorUtils.getSupportedDrivers(notebookEditor.project)

  private val currentExecutorProvider: NoteExecutable?
    get() {
      val configId = NotebookFileUtil.getConfigId(noteVirtualFile)
      return executorsProvider.firstOrNull { it.getExternalId() == configId }
    }

  private var curExecutor: NoteExecutor? = null

  init {
    NotebookFileUtil.setContainerId(notebookEditor.file, System.identityHashCode(notebookEditor).toString())
    Disposer.register(this, LocalNoteActionHandler(notebookEditor))
    ZeppelinNoteLoadPromise.makeLoaded(notebookEditor, null)
    noteVirtualFile.putUserData(CONTAINER_KEY, this)

    collectStatistic()
    createConnection()

    stopAllExecutingCells(notebook)
  }

  override fun dispose() = disconnectExecutor()

  fun setNewExecutor(noteExecutable: NoteExecutable?) = executeOnPooledThread {
    disconnectExecutor()
    if (noteExecutable != null)
      NotebookFileUtil.setConfigId(noteVirtualFile, noteExecutable.getExternalId())
    createConnection()
  }

  private fun createConnection() {
    NotebookFileUtil.setNotebookId(noteVirtualFile, "")

    val executor = currentExecutorProvider?.createExecutor(notebookEditor) ?: let {
      updateToolbarActions(null)
      return
    }

    Disposer.register(this, executor)
    Disposer.register(executor, LocalNoteExecuteController(notebookEditor, executor))
    Disposer.register(notebookEditor, executor)
    curExecutor = executor
    updateToolbarActions(executor)
    executeOnPooledThread {
      executor.connect()
    }
  }

  private fun updateToolbarActions(executor: NoteExecutor?) {
    val executorActions = executor?.getNoteExecutorToolbarActions() ?: emptyList()
    val instanceManagementAction = ExecutorManagementAction(notebookEditor.file)
    @Suppress("HardCodedStringLiteral")
    val statusAction = object : ToolbarLabelAction() {
      override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = curExecutor == executor && executor != null
        if (!e.presentation.isEnabledAndVisible)
          return
        e.presentation.text = when (val connectionStatus = executor?.connectionStatus) {
          is ConnectedConnectionStatus -> ZepMessagesBundle.message("local.note.status.connected.text")
          is ConnectingConnectionStatus -> ZepMessagesBundle.message("local.note.status.connected.text")
          is FailedConnectionStatus -> ZepMessagesBundle.message("local.note.status.connect.disconnected") +
                                       (connectionStatus.getException()?.toPresentableText()?.let { " $it" } ?: "")
          null -> ""
        }
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }
    val actions = listOf<AnAction>(Separator.create(), instanceManagementAction, statusAction) + if (executorActions.isEmpty())
      emptyList()
    else
      listOf(Separator.create()) + executorActions

    notebookEditor.setExecutorToolbarActions(actions)
  }

  private fun disconnectExecutor() {
    curExecutor?.let {
      curExecutor = null
      Disposer.dispose(it)
    }

    stopAllExecutingCells(notebook)
  }

  private fun collectStatistic() = ZeppelinNotebookUsageCollector.logNoteOpened(project, notebook, noteVirtualFile, false)

  companion object {
    private val CONTAINER_KEY = Key<LocalNoteContainer>("NOTE_CONTAINER")

    fun stopAllExecutingCells(notebook: ZeppelinNotebook) = invokeAndWaitIfNeeded {
      notebook.performModification {
        notebook.cells.filter { it.isLaunched }.forEach {
          it.status = CellStatus.ABORT
        }
      }
    }

    fun getContainer(noteVirtualFile: NotebookVirtualFile) = noteVirtualFile.getUserData(CONTAINER_KEY)
  }
}