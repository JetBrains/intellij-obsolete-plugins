package com.jetbrains.bigdatatools.common.editor.decorator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.bigdatatools.common.data.StructuredFilesUtil
import com.jetbrains.bigdatatools.common.editor.decorator.file.RfsInMemoryFile
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.rfs.localcache.RfsDownloadedStorageManager
import com.jetbrains.bigdatatools.common.rfs.view.FileTypeViewerManager
import com.jetbrains.bigdatatools.common.rfs.view.ViewUtil
import com.jetbrains.bigdatatools.common.table.editor.LoadingLightVirtualFile
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.util.SizeUtils

class LoadFullFileAction : DumbAwareAction() {
  init {
    templatePresentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    val localVirtualFile = e.getData(PlatformCoreDataKeys.FILE_EDITOR)?.file

    if (localVirtualFile == null) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    // Only for CSV Currently, we have too many problems with reading parquet.
    if (!StructuredFilesUtil.isTextTableExtension(localVirtualFile.extension ?: "")) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    val remoteFileInfo = getRemoteFileInfo(e)

    if (remoteFileInfo != null && remoteFileInfo.length != localVirtualFile.length) {
      e.presentation.isEnabledAndVisible = true
      e.presentation.text = MessagesBundle.message("rfs.load.full.file.action.text", SizeUtils.toString(remoteFileInfo.length))
      return
    }

    e.presentation.isEnabledAndVisible = false
  }

  private fun getRemoteFileInfo(e: AnActionEvent): FileInfo? {
    val remoteVirtualFile = e.getData(PlatformCoreDataKeys.VIRTUAL_FILE)
    if (remoteVirtualFile is LoadingLightVirtualFile) {
      return remoteVirtualFile.underlyingFile
    }

    val localVirtualFile = e.getData(PlatformCoreDataKeys.FILE_EDITOR)?.file
    val actualFile = if (localVirtualFile is RfsInMemoryFile)
      localVirtualFile.cachedFile
    else localVirtualFile

    if (actualFile != null) {
      val metadata = service<RfsDownloadedStorageManager>().findRelevantMetadata(actualFile) ?: return null
      val driver = DriverManager.getDriverById(e.project, metadata.driverId) ?: return null
      return driver.getFileStatus(RfsPath(metadata.path, isDirectory = false)).getOrElse { null }
    }

    return null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val remoteFileInfo = getRemoteFileInfo(e) ?: return
    val virtualFile = e.getData(PlatformCoreDataKeys.VIRTUAL_FILE)
    if (virtualFile != null) {
      val fileEditorManager = FileEditorManager.getInstance(project)
      fileEditorManager.closeFile(virtualFile)
    }
    ViewUtil.downloadAndOpenFile(project, remoteFileInfo) {
      FileTypeViewerManager.getInstance(project).openViewer(remoteFileInfo, true)
    }
  }
}