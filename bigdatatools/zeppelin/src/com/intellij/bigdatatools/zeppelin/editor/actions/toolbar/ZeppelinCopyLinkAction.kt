package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.zeppelin.components.instance.ZeppelinConnectionManager
import com.intellij.bigdatatools.zeppelin.editor.NoteActionsIds
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.file.ZeppelinRemoteFile
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.StatusBarEx
import com.jetbrains.bigdatatools.common.table.ClipboardUtils
import org.jetbrains.annotations.Nls

class ZeppelinCopyLinkAction(val zeppelinEditor: ZeppelinEditor?)
  : RemoteNotebookDumbAwareAction(zeppelinEditor, NoteActionsIds.OPEN_IN_EXTERNAL_BROWSER,
                                  ZepMessagesBundle.message("action.copy.link.title"),
                                  ZepMessagesBundle.message("action.copy.link.desc"),
                                  AllIcons.Actions.Copy) {
  constructor() : this(null)

  override fun actionPerformed(e: AnActionEvent) {
    super.actionPerformed(e)
    val actualEditor = actualEditor(e) ?: return
    val cacheConnection = ZeppelinConnectionManager.getNoteConnectionByEditor(actualEditor) ?: return
    val httpUrl = cacheConnection.tunnelUri ?: cacheConnection.config.getFullHttpUrl()
    val noteUrl = ZeppelinConnectionData.getUrlToNote(httpUrl, actualEditor.note.id)
    ClipboardUtils.setStringContent(noteUrl)
    setStatusBarText(actualEditor.project, IdeBundle.message("message.path.to.fqn.has.been.copied", noteUrl))
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isVisible = e.presentation.isVisible && actualEditor(e)?.file?.originFile is ZeppelinRemoteFile
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private fun setStatusBarText(project: Project, @Nls message: String?) {
    val statusBar = WindowManager.getInstance().getStatusBar(project) as StatusBarEx
    statusBar.info = message
  }
}