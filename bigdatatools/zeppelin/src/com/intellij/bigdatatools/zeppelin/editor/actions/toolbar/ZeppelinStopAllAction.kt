package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.editor.NoteActionsIds
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent

class ZeppelinStopAllAction(zeppelinEditor: ZeppelinEditor?) :
  ZeppelinEditorDumbAwareAction(zeppelinEditor,
                                NoteActionsIds.STOP_ALL,
                                ZepMessagesBundle.message("action.stop.all"),
                                null,
                                ZeppelinIcons.STOP_EXECUTION) {
  constructor() : this(null)

  override fun actionPerformed(e: AnActionEvent) {
    super.actionPerformed(e)

    actualEditor(e)?.actionNotify {
      it.stopAll()
    }
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabled = e.presentation.isEnabled && actualEditor(e)?.note?.hasRunningCells == true
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}