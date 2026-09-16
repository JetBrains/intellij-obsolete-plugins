package com.intellij.bigdatatools.zeppelin.drivers.actions

import com.intellij.bigdatatools.zeppelin.drivers.fileinfo.ZeppelinFileInfo
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread

class ClearOutputAction : ZeppelinPaneAction() {

  override fun actionPerformed(e: AnActionEvent): Unit = withRfsPane(e) {
    val fileInfo = getSelectedFileInfo() as? ZeppelinFileInfo ?: return
    val result = Messages.showYesNoDialog(pane.project,
                                          ZepMessagesBundle.message("action.clear.output.ask"),
                                          ZepMessagesBundle.message("action.confirmation"),
                                          Messages.getQuestionIcon())
    if (result != Messages.YES)
      return

    executeOnPooledThread {
      fileInfo.clearOutput()
    }
  }

  override fun update(e: AnActionEvent): Unit = withRfsPane(e) {
    e.presentation.isVisible = isZeppelin() && isSingleDriverSelect() && !isFolder()
    e.presentation.isEnabled = e.presentation.isVisible && isLoaded()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}