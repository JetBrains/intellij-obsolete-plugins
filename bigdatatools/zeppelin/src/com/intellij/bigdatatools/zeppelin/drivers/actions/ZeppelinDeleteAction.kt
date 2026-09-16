package com.intellij.bigdatatools.zeppelin.drivers.actions

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.zeppelin.drivers.actions.ZeppelinPaneAction.Companion.isInTrash
import com.intellij.bigdatatools.zeppelin.drivers.actions.ZeppelinPaneAction.Companion.isZeppelin
import com.intellij.bigdatatools.zeppelin.drivers.fileinfo.ZeppelinFileInfo
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.fileinfo.ErrorResult
import com.jetbrains.bigdatatools.common.rfs.driver.getPresentableName
import com.jetbrains.bigdatatools.common.rfs.projectview.actions.RfsDeleteActionBase

class ZeppelinDeleteAction : RfsDeleteActionBase() {
  override fun getProgressText(msg: String) = MessagesBundle.message("fs.task.delete.text", msg)

  override fun shouldDelete(files: List<FileInfo>, project: Project?): Boolean =
    askConfirmation(project, ZepMessagesBundle.message("action.delete.from.trash.message"))

  override fun doDelete(fileInfo: FileInfo, errors: MutableList<Pair<FileInfo, Throwable>>, indicator: ProgressIndicator) {
    if (fileInfo !is ZeppelinFileInfo) {
      errors.add(fileInfo to IllegalArgumentException(
        ZepMessagesBundle.message("rfs.action.delete.error.non.zeppelin", fileInfo.getPresentableName())))
      return
    }
    //TODO we actually can report errors with more precision here if we alter removeFromTrash
    val removeResult = fileInfo.removeFromTrash()
    if (removeResult is ErrorResult) errors.add(Pair(fileInfo, removeResult.exception ?: noThrowableException(fileInfo)))
    fileInfo.driver.fileInfoManager.waitDisappear(fileInfo.path)
  }

  override fun update(e: AnActionEvent) = withRfsPane(e) {
    e.presentation.isVisible = isZeppelin() && isInTrash()
    e.presentation.isEnabled = e.presentation.isVisible && isLoaded()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
