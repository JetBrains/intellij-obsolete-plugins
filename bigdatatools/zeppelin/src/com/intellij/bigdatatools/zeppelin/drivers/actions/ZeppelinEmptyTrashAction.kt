package com.intellij.bigdatatools.zeppelin.drivers.actions

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class ZeppelinEmptyTrashAction : ZeppelinPaneAction() {

  override fun actionPerformed(e: AnActionEvent) {
    withRfsPane(e) {
      val rfsNode = getSelectedDriverNode() ?: return
      val driver = rfsNode.driver as ZeppelinDriver
      val result = Messages.showYesNoDialog(project, ZepMessagesBundle.message("rfs.action.emptyTrash.confirmation.message"),
                                            ZepMessagesBundle.message("rfs.action.emptyTrash.confirmation.title"),
                                            Messages.getWarningIcon())
      if (result != Messages.YES) return
      if (!driver.emptyTrash()) {
        @Suppress("DialogTitleCapitalization")
        NotificationUtils.reportActionFailure(ZepMessagesBundle.message("rfs.action.emptyTrash.fail.notification.title"))
      }
      driver.fileInfoManager.waitDisappear(rfsNode.rfsPath)
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