package com.intellij.bigdatatools.zeppelin.drivers.actions

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

//to restore file infos, we actually delete nodes from trash, so from pane's point of view this is a delete too
class ZeppelinRestoreAction : RfsDeleteActionBase() {
  override fun getProgressText(msg: String) = ZepMessagesBundle.message("progress.title.restoring", msg)

  override fun shouldDelete(files: List<FileInfo>, project: Project?): Boolean = true

  override fun doDelete(fileInfo: FileInfo, errors: MutableList<Pair<FileInfo, Throwable>>, indicator: ProgressIndicator) {
    if (fileInfo !is ZeppelinFileInfo) {
      errors.add(
        fileInfo to IllegalStateException(ZepMessagesBundle.message("rfs.action.restore.error.non.zeppelin", fileInfo.getPresentableName())
        ))
      return
    }
    val result = fileInfo.restore()
    if (result is ErrorResult)
      errors.add(Pair(fileInfo, result.exception ?: noThrowableException(fileInfo)))
    fileInfo.driver.fileInfoManager.waitAppear(fileInfo.path)
  }

  override fun noThrowableException(info: FileInfo) = IllegalStateException(
    ZepMessagesBundle.message("rfs.action.restore.failed.no.exception", info.getPresentableName()))

  override fun update(e: AnActionEvent) = withRfsPane(e) {
    e.presentation.isVisible = isZeppelin() && isInTrash()
    e.presentation.isEnabled = e.presentation.isVisible && isLoaded()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
