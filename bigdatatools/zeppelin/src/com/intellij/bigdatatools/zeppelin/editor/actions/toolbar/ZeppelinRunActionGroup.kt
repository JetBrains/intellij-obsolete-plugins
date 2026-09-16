package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinRunAllAboveAction
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinRunAllBelowAction
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup

class ZeppelinRunActionGroup(zeppelinEditor: ZeppelinEditor) : DefaultActionGroup(
  ZeppelinRunAllAction(zeppelinEditor),
  ActionManager.getInstance().getAction(ZeppelinRunAllAboveAction.ID),
  ActionManager.getInstance().getAction(ZeppelinRunAllBelowAction.ID)) {

  init {
    isPopup = true
    templatePresentation.text = ZepMessagesBundle.message("run.all.actions")
    templatePresentation.icon = ZeppelinIcons.RUN_ALL
  }

  override fun isDumbAware(): Boolean = true
}