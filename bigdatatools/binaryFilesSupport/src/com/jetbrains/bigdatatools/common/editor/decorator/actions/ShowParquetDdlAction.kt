package com.jetbrains.bigdatatools.common.editor.decorator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.bigdatatools.common.rfs.localcache.RfsDownloadedStorageManager
import com.jetbrains.bigdatatools.common.table.editor.getFileMetadata
import com.jetbrains.bigdatatools.common.ui.ParquetDDLDialog

class ShowParquetDdlAction : DumbAwareAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    val virtualFile = e.getData(PlatformCoreDataKeys.FILE_EDITOR)?.file
    e.presentation.isEnabledAndVisible = virtualFile != null && RfsDownloadedStorageManager.hasFileMetadata(virtualFile)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val virtualFile = e.getData(PlatformCoreDataKeys.FILE_EDITOR)?.file ?: return
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    ParquetDDLDialog(project, virtualFile.nameWithoutExtension, getFileMetadata(virtualFile)).show()
  }
}