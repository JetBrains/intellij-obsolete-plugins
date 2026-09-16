package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.editor.NoteActionsIds
import com.intellij.bigdatatools.zeppelin.editor.PARENT_ZEPPELIN_EDITOR
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.statistics.ZeppelinNotebookUsageCollector
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import javax.swing.Icon

/**
 * Zeppelin Editor toolbar actions
 */
abstract class ZeppelinEditorDumbAwareAction(val editor: ZeppelinEditor?,
                                             val id: NoteActionsIds, // This id required only for statistics collection.
                                             @NlsActions.ActionText text: String?,
                                             @NlsActions.ActionDescription description: String?,
                                             icon: Icon?) : DumbAwareAction(text, description, icon) {
  fun actualContext(e: AnActionEvent): Pair<ZeppelinEditor, Project>? {
    if (editor != null) return Pair(editor, editor.project)

    val project = e.project ?: return null
    val psiAwareEditor = e.getData(PlatformCoreDataKeys.FILE_EDITOR)?.getUserData(PARENT_ZEPPELIN_EDITOR)?.get() ?: return null

    return Pair(psiAwareEditor, project)
  }

  fun actualEditor(e: AnActionEvent): ZeppelinEditor? = actualContext(e)?.first

  override fun actionPerformed(e: AnActionEvent) {
    collectStatistic(e)
  }

  var isConnected = true

  private fun collectStatistic(e: AnActionEvent) {
    val (actualEditor, project) = actualContext(e) ?: return
    val notebookVirtualFile = actualEditor.getSourceEditor().file as NotebookVirtualFile
    val zeppelinNotebook = notebookVirtualFile.notebook as ZeppelinNotebook
    val isRemote = NotebookFileUtil.isRemote(notebookVirtualFile)
    ZeppelinNotebookUsageCollector.logToolbarActionClickedEvent(project, id, zeppelinNotebook, isRemote)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabled = isConnected
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}