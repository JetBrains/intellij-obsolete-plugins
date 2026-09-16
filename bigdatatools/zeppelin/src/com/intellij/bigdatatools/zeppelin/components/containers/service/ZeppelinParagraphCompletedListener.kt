package com.intellij.bigdatatools.zeppelin.components.containers.service

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinParagraphTitleController
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinFileTypeViewer
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.ui.SystemNotifications

class ZeppelinParagraphCompletedListener(val editor: ZeppelinEditor) : Disposable {
  private val notebook = editor.note

  val config: ZeppelinConnectionData?
    get() {
      val configId = NotebookFileUtil.getConfigId(editor.file) ?: return null
      return ZeppelinDriverManager.getDriver(editor.project, configId)?.connectionData
    }

  private val listener = object : NotebookChangeListener {
    override fun onEvent(notebookEvent: NotebookEvent) {
      val remoteConfig = config ?: return
      if (!remoteConfig.systemNotificationEnabled)
        return
      if (notebookEvent !is CellChanged || NotebookSchema.cellStatus !in notebookEvent.changedFields)
        return
      if (notebookEvent.cell.status !in setOf(CellStatus.ERROR, CellStatus.ABORT, CellStatus.FINISHED))
        return

      val cell = notebookEvent.cell as ZeppelinCell
      val started = cell.dateStarted?.time ?: return
      val finished = cell.dateFinished?.time ?: return
      val diff = finished - started
      if (diff < remoteConfig.notifyAfter * 1000)
        return

      @Suppress("DialogTitleCapitalization")
      val title = ZepMessagesBundle.message("note.execution.finished")
      val message = ZepMessagesBundle.message("system.notification.text.cell.in.note.finished",
                                              ZeppelinParagraphTitleController.generateTitle(cell),
                                              editor.file.name, cell.status)

      SystemNotifications.getInstance().notify(NotificationGroupManager.getInstance().getNotificationGroup("Job Notification").displayId,
                                               title, message)

      val selectedEditor = FileEditorManager.getInstance(editor.project).selectedEditor
      if (editor == selectedEditor)
        return

      NotificationUtils.notifySuccess(message, title, ZepMessagesBundle.message("action.GotoParagraph.text")) {
        val project = editor.project
        val configId = config?.innerId ?: return@notifySuccess false
        val driver = ZeppelinDriverManager.getDriver(project = project, configId) ?: return@notifySuccess false
        val notePath = driver.getNotePathById(notebook.id) ?: return@notifySuccess false

        ZeppelinFileTypeViewer.Util.INSTANCE.openFile(project, driver, notePath, true, cell.id)
        true
      }
    }
  }

  init {
    notebook.addNotebookChangeListener(listener)
  }

  override fun dispose() = notebook.removeNotebookChangeListener(listener)
}