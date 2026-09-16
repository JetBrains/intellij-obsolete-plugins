package com.intellij.bigdatatools.zeppelin.drivers.actions

import com.intellij.bigdatatools.zeppelin.drivers.actions.ZeppelinPaneAction.Companion.isNotInTrash
import com.intellij.bigdatatools.zeppelin.drivers.actions.ZeppelinPaneAction.Companion.isZeppelin
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.fileinfo.ErrorResult
import com.jetbrains.bigdatatools.common.rfs.driver.getPresentableName
import com.jetbrains.bigdatatools.common.rfs.projectview.actions.RfsDeleteActionBase

class ZeppelinMoveToTrashAction : RfsDeleteActionBase() {
  override fun getProgressText(msg: String) = ZepMessagesBundle.message("progress.title.moving.to.trash", msg)

  override fun shouldDelete(files: List<FileInfo>, project: Project?): Boolean =
    askConfirmation(project, ZepMessagesBundle.message("move.to.trash.desc"))

  override fun doDelete(fileInfo: FileInfo, errors: MutableList<Pair<FileInfo, Throwable>>, indicator: ProgressIndicator) {
    val status = fileInfo.delete()
    if (status is ErrorResult)
      errors.add(fileInfo to (status.exception ?: noThrowableException(fileInfo)))
    fileInfo.driver.fileInfoManager.waitDisappear(fileInfo.path)
  }

  override fun noThrowableException(info: FileInfo) = IllegalStateException(
    ZepMessagesBundle.message("rfs.action.move.to.trash.failed.no.exception", info.getPresentableName()))

  override fun update(e: AnActionEvent) = withRfsPane(e) {
    e.presentation.isVisible = isZeppelin() && isNotInTrash() && !isMount()
    e.presentation.isEnabled = e.presentation.isVisible && isLoaded()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}