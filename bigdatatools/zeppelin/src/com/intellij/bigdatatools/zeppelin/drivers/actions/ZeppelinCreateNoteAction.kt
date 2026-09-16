package com.intellij.bigdatatools.zeppelin.drivers.actions

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.rfs.node.ZeppelinRfsTreeNode
import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.jetbrains.bigdatatools.common.rfs.ui.BdtMessages
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.jetbrains.bigdatatools.common.util.toPresentableText

class ZeppelinCreateNoteAction : ZeppelinPaneAction() {

  override fun actionPerformed(e: AnActionEvent): Unit = withRfsPane(e) {
    val node = getSelectedDriverNode() ?: return
    if (node !is ZeppelinRfsTreeNode) return

    val selectedNode = if (node.zepRfsPath.isFile) node.parent as ZeppelinRfsTreeNode else node
    val driver = selectedNode.zepDriver
    val fileInfo = selectedNode.fileInfo ?: return
    createNote(fileInfo.path as ZeppelinRfsPath, driver, project)
  }

  private fun createNote(rfsPath: ZeppelinRfsPath, driver: ZeppelinDriver, project: Project) {
    val newName = BdtMessages.showInputDialogWithDescription(project,
                                                             ZepMessagesBundle.message("show.create.note.title"),
                                                             ZepMessagesBundle.message("show.create.note.label"),
                                                             ZepMessagesBundle.message("show.create.note.desc"),
                                                             suggestNewNoteName(rfsPath, driver).name,
                                                             RelativeNoteNameValidator(driver, rfsPath)) ?: return


    try {
      val newPath = rfsPath.addRelative(newName, isDirectory = false)
      val path = ZeppelinRfsPath.createRfsPath("", newPath.stringRepresentation())

      driver.addPromiseOpenNote(project, path)
      executeOnPooledThread {
        try {
          driver.createNote(path)
        }
        catch (t: Throwable) {
          NotificationUtils.notifyException(t)
        }
      }
    }
    catch (t: Throwable) {
      Messages.showErrorDialog(t.toPresentableText(), ZepMessagesBundle.message("action.create.note.error"))
    }
  }

  override fun update(e: AnActionEvent): Unit = withRfsPane(e) {
    e.presentation.isVisible = isZeppelin() && isNotInTrash()
    e.presentation.isEnabled = e.presentation.isVisible && isSingleDriverSelect() && isLoaded()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}