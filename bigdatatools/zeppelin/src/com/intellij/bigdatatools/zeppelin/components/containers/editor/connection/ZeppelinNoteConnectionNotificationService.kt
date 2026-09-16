package com.intellij.bigdatatools.zeppelin.components.containers.editor.connection

import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.controller.ZeppelinNoteController
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.ZeppelinEditorDumbAwareAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Key
import com.intellij.ui.EditorNotifications


class ZeppelinNoteConnectionNotificationService(controller: ZeppelinNoteController) : Disposable {
  private val zeppelinEditor = controller.zeppelinEditor
  private val cachedConnection = controller.cachedConnection
  private val editor = zeppelinEditor.editor
  private val project = editor.project ?: throw Exception("Editor does not have project")
  private val noteFile = zeppelinEditor.file
  private val originFile = noteFile.originFile

  private val connectionListener = object : ZeppelinConnectionListener {
    override fun onConnected() = updateEditor(isConnected = true)
    override fun onDisconnected(statusCode: Int?, reason: String?) = updateEditor(isConnected = false)
  }

  init {
    cachedConnection.addListener(connectionListener)
    updateToolbar(cachedConnection.isConnected())
  }

  fun updateEditor(isConnected: Boolean) {
    editor.document.setReadOnly(!isConnected)
    updateToolbar(isConnected)
    originFile.putUserData(IS_CONNECTED, isConnected)
    EditorNotifications.getInstance(project).updateNotifications(originFile)
  }

  private fun updateToolbar(isConnected: Boolean) {
    zeppelinEditor.toolbar.actions.filterIsInstance<ZeppelinEditorDumbAwareAction>().forEach {
      it.isConnected = isConnected
    }
  }

  override fun dispose() = cachedConnection.removeListener(connectionListener)

  companion object {
    val IS_CONNECTED = Key<Boolean>("IS_CONNECTED_NOTE")
  }
}