package com.intellij.bigdatatools.zeppelin.drivers.actions

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class ZeppelinRestoreAllAction : ZeppelinPaneAction() {
  override fun actionPerformed(e: AnActionEvent) {
    withRfsPane(e) {
      val rfsNode = getSelectedDriverNode() ?: return
      val driver = rfsNode.driver as ZeppelinDriver
      val result = Messages.showYesNoDialog(project,
                                            ZepMessagesBundle.message("action.restore.note.message"),
                                            ZepMessagesBundle.message("action.confirmation"),
                                            Messages.getQuestionIcon())
      if (result != Messages.YES)
        return

      if (!driver.restoreAllFromTrash()) {
        NotificationUtils.reportActionFailure(ZepMessagesBundle.message("action.restore.note.failure"))
      }
      driver.fileInfoManager.waitAppear(rfsNode.rfsPath)
    }
  }

  override fun update(e: AnActionEvent) {
    withRfsPane(e) {
      e.presentation.isVisible = isSingleDriverSelect() && isTrash()
      e.presentation.isEnabled = e.presentation.isVisible && isLoaded()
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}