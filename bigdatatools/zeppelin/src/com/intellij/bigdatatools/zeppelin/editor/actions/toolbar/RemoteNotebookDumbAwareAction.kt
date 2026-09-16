package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.zeppelin.editor.NoteActionsIds
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * Action is only visible for remote notebooks.
 */
abstract class RemoteNotebookDumbAwareAction(editor: ZeppelinEditor?,
                                             id: NoteActionsIds,
                                             @NlsActions.ActionText text: String?,
                                             @NlsActions.ActionDescription description: String?,
                                             icon: Icon?) : ZeppelinEditorDumbAwareAction(editor, id, text, description, icon) {
  private fun getSelectedConfig(virtualFile: VirtualFile) =
    virtualFile.getCopyableUserData(ZeppelinFileType.SHARED_NOTEBOOK_SELECTED_NAME)

  override fun update(e: AnActionEvent) {
    super.update(e)
    
    val virtualFile = actualEditor(e)?.file
    if (virtualFile == null) {
      e.presentation.isVisible = false
      return
    }

    val selectedConfig = getSelectedConfig(virtualFile)
    e.presentation.isVisible = selectedConfig == null
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}