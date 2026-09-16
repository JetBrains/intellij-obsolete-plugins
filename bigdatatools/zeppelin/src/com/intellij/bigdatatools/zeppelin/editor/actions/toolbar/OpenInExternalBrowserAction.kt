package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.zeppelin.components.instance.ZeppelinConnectionManager
import com.intellij.bigdatatools.zeppelin.editor.NoteActionsIds
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.file.ZeppelinRemoteFile
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent

class OpenInExternalBrowserAction(val zeppelinEditor: ZeppelinEditor?)
  : RemoteNotebookDumbAwareAction(zeppelinEditor, NoteActionsIds.OPEN_IN_EXTERNAL_BROWSER,
                                  ZepMessagesBundle.message("action.open.in.browser"),
                                  null,
                                  AllIcons.General.Web) {
  constructor() : this(null)

  override fun actionPerformed(e: AnActionEvent) {
    super.actionPerformed(e)
    val actualEditor = actualEditor(e) ?: return
    val cacheConnection = ZeppelinConnectionManager.getNoteConnectionByEditor(actualEditor) ?: return
    val httpUrl = cacheConnection.tunnelUri ?: cacheConnection.config.getFullHttpUrl()
    val noteUrl = ZeppelinConnectionData.getUrlToNote(httpUrl, actualEditor.note.id)
    BrowserUtil.browse(noteUrl)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isVisible = e.presentation.isVisible && actualEditor(e)?.file?.originFile is ZeppelinRemoteFile
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}