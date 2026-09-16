package com.intellij.bigdatatools.zeppelin.components.containers

import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.controller.ZeppelinNoteController
import com.intellij.bigdatatools.zeppelin.components.containers.editor.connection.ZeppelinEmptyBindingsController
import com.intellij.bigdatatools.zeppelin.components.containers.editor.connection.ZeppelinLoadNoteHandler
import com.intellij.bigdatatools.zeppelin.components.containers.editor.connection.ZeppelinNoteConnectionNotificationService
import com.intellij.bigdatatools.zeppelin.components.containers.editor.connection.ZeppelinNoteGoToTargetListener
import com.intellij.bigdatatools.zeppelin.components.containers.service.ZeppelinExecutionListener
import com.intellij.bigdatatools.zeppelin.components.instance.ZeppelinConnectionManager
import com.intellij.bigdatatools.zeppelin.components.service.ZeppelinServerErrorNotifier
import com.intellij.bigdatatools.zeppelin.controllers.editor.BlockLastCellController
import com.intellij.bigdatatools.zeppelin.controllers.remote.SynchronizeRemoteNoteComponent
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.ZeppelinToolbarActionsProvider
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.interpreters.components.InterpretersActionsHandler
import com.intellij.bigdatatools.zeppelin.interpreters.components.SparkInterpreterPrecodeHandler
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.bigdatatools.zeppelin.statistics.ZeppelinNoteStatisticListener
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsNoteController
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer

/**
 * A class, which represent the opened synchronized notebook in IDEA
 */
class ZeppelinRemoteNoteContainer(val zeppelinEditor: ZeppelinEditor,
                                  private var connectionManager: ZeppelinConnectionManager) : Disposable {
  val project: Project = zeppelinEditor.editor.project ?: error("Project is not found")

  private val noteFile = zeppelinEditor.file
  private val originVirtualFile = noteFile.originFile
  private val noteId: String = NotebookFileUtil.getNotebookId(originVirtualFile) ?: error("Cannot find note id for note")

  private val cacheConnection = connectionManager.createNoteConnectionForEditor(noteId, zeppelinEditor)

  private val controller = ZeppelinNoteController(project, cacheConnection, zeppelinEditor)

  init {
    initToolbarActions()

    val synchronizeRemoteNoteComponent = SynchronizeRemoteNoteComponent(zeppelinEditor, cacheConnection)
    //We need register  remoteNoteController to connection because we need to send a sync paragraph message before connection close
    Disposer.register(cacheConnection, synchronizeRemoteNoteComponent)
    //Register to this because connection can be 1 for several notes in different projects
    Disposer.register(this, synchronizeRemoteNoteComponent)

    Disposer.register(this, SparkInterpreterPrecodeHandler(noteFile, cacheConnection))
    Disposer.register(this, InterpretersActionsHandler(zeppelinEditor))
    Disposer.register(this, ZeppelinServerErrorNotifier(project, cacheConnection))
    Disposer.register(this, ZeppelinLoadNoteHandler(controller))
    Disposer.register(this, ZeppelinNoteGoToTargetListener(controller))
    Disposer.register(this, ZeppelinNoteConnectionNotificationService(controller))
    Disposer.register(this, ZeppelinMarkerResolveContributor(cacheConnection, zeppelinEditor))
    Disposer.register(this, Disposable {
      connectionManager.destroyNoteCachedConnectionForEditor(noteId, zeppelinEditor)
    })
    Disposer.register(this, ZeppelinEmptyBindingsController(controller))
    Disposer.register(this, ZeppelinNoteStatisticListener(controller))
    Disposer.register(this, ZeppelinExecutionListener(zeppelinEditor, cacheConnection))
    Disposer.register(this, BlockLastCellController(zeppelinEditor, cacheConnection))

    val isZtoolsEnabled = ZeppelinDriverManager.getDriver(project,
                                                          connectionManager.config.innerId)!!.connectionData.isZtoolsEnabled == true
    if (isZtoolsEnabled) {
      Disposer.register(this, ZtoolsNoteController(zeppelinEditor, cacheConnection))
    }

    cacheConnection.addListener(object : ZeppelinConnectionListener {
      override fun onConnected() {
        val versionInt = cacheConnection.zeppelinInfo?.versionInt ?: let {
          zeppelinEditor.executionProgressController.disabled = true
          return
        }
        zeppelinEditor.executionProgressController.disabled = versionInt < 8
      }

      override fun onZeppelinInfoChange(newInfo: ZeppelinInfo?) {
        zeppelinEditor.executionProgressController.disabled = newInfo != null && newInfo.versionInt < 8
      }

      override fun onDisconnected(statusCode: Int?, reason: String?) {
        zeppelinEditor.executionProgressController.disabled = true
      }
    })

    cacheConnection.refreshConnectionAsync(false, project)
  }

  private fun initToolbarActions() {
    zeppelinEditor.setExecutorToolbarActions(ZeppelinToolbarActionsProvider.createActionsForRemote(zeppelinEditor))
  }

  override fun dispose() = Unit
}