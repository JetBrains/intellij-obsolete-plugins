package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.editor.NoteActionsIds
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent

class ZeppelinClearAllOutputAction(zeppelinEditor: ZeppelinEditor) :
  ZeppelinEditorDumbAwareAction(zeppelinEditor,
                                NoteActionsIds.CLEAR_ALL_OUTPUT,
                                ZepMessagesBundle.message("action.clear.outputs.all"),
                                null,
                                ZeppelinIcons.CLEAR_OUTPUTS) {
  override fun actionPerformed(e: AnActionEvent) {
    super.actionPerformed(e)

    actualContext(e)?.first?.actionNotify {
      it.clearAllOutput()
    }
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabled = e.presentation.isEnabled || actualEditor(e)?.file?.let { !NotebookFileUtil.isRemote(it) } == true
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}