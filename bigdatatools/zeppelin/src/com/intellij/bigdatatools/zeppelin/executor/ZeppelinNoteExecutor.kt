package com.intellij.bigdatatools.zeppelin.executor

import com.intellij.bigdatatools.notebooks.core.api.editor.NotebookEditor
import com.intellij.bigdatatools.notebooks.core.api.executor.NoteExecutorBase
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.ZeppelinMarkerResolveContributor
import com.intellij.bigdatatools.zeppelin.controllers.remote.ZeppelinLocalNoteServerManager
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.ZeppelinToolbarActionsProvider
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.inlay.InlaysZeppelinExecutionListener
import com.intellij.bigdatatools.zeppelin.interpreters.components.InterpretersActionsHandler
import com.intellij.bigdatatools.zeppelin.interpreters.components.SparkInterpreterPrecodeHandler
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsNoteController
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.rfs.driver.DriverConnectionStatus
import com.jetbrains.bigdatatools.common.updater.BDTPluginUtil
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.util.concurrent.atomic.AtomicBoolean

class ZeppelinNoteExecutor(val driver: ZeppelinDriver, val noteEditor: NotebookEditor) : NoteExecutorBase() {
  private val isDisposed = AtomicBoolean(false)

  val project = noteEditor.project
  val connectionManager = driver.connectionManager
  override val presentableName: String = driver.presentableName
  override val id: String = driver.getExternalId()

  override val connectionStatus: DriverConnectionStatus
    get() = connectionManager.instanceConnection.getConnectionStatus()

  private val containerId = NotebookFileUtil.getContainerId(noteEditor.file) ?: ""
  val noteConnection = connectionManager.createNoteConnectionForEditor(containerId, noteEditor, noteId = "")

  private val serverManager = ZeppelinLocalNoteServerManager(noteEditor as ZeppelinEditor, noteConnection, this)

  init {
    NotebookFileUtil.setConfigSpecification(noteEditor.file, "")

    Disposer.register(driver, this)
    Disposer.register(this, Disposable {
      connectionManager.destroyNoteCachedConnectionForEditor(containerId, noteEditor)
    })

    noteConnection.addListener(object : ZeppelinConnectionListener {
      override fun onConnected() {
        if (!connectionManager.isConnected()) return
        val tempNoteId = connectionManager.instanceConnection.api.getTempNotebook()?.id ?: return
        NotebookFileUtil.setNotebookId(noteEditor.file, tempNoteId)

        if (noteConnection.noteId != tempNoteId) {
          noteConnection.updateNoteId(tempNoteId)
          return
        }

        invokeDependencyResolverUpdate()
      }
    })

    Disposer.register(this, serverManager)
    noteEditor as ZeppelinEditor
    Disposer.register(this, SparkInterpreterPrecodeHandler(noteEditor.file, noteConnection))
    Disposer.register(this, InterpretersActionsHandler(noteEditor))
    Disposer.register(this, ZeppelinMarkerResolveContributor(noteConnection, noteEditor))

    invokeLater {
      if (isDisposed.get())
        return@invokeLater
      val isZtoolsEnabled = ZeppelinDriverManager.getDriver(project,
                                                            connectionManager.config.innerId)?.connectionData?.isZtoolsEnabled == true
      if (isZtoolsEnabled && BDTPluginUtil.isDatabaseEnabled()) {
        Disposer.register(this, ZtoolsNoteController(noteEditor, noteConnection))
      }

    }

    NotebookEditorUtils.refreshHighlight(noteEditor)
  }

  override fun dispose() {
    isDisposed.set(true)
    super.dispose()
  }

  override fun getNoteExecutionListeners() = listOf(InlaysZeppelinExecutionListener(),
                                                    (noteEditor as ZeppelinEditor).executionProgressController)

  override fun connect() {
    noteConnection.refreshConnectionAsync(false, project)
  }

  override fun disconnect() = noteConnection.disconnect()

  override fun runCell(cell: NotebookCell) = serverManager.executeCell(cell as ZeppelinCell)

  override fun stopCell(cell: NotebookCell) = serverManager.stopCell(cell as ZeppelinCell)

  override fun getNoteExecutorToolbarActions() = ZeppelinToolbarActionsProvider.createActionsForLocal()

  private fun invokeDependencyResolverUpdate() = invokeLater {
    val projectRootManager = ProjectRootManager.getInstance(project)
    runWriteAction {
      (projectRootManager as ProjectRootManagerEx).makeRootsChange({}, false, true)
    }
  }
}