package com.jetbrains.bigdatatools.view.filetypes

import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.bigdatatools.coreUi.util.messageOrDefault
import com.intellij.charts.dataframe.DataFrame
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.WeighedFileEditorProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.bigdatatools.common.editor.BdiDecoratableEditor
import com.jetbrains.bigdatatools.common.rfs.driver.local.LocalDriverManager
import com.jetbrains.bigdatatools.common.rfs.localcache.RfsFileContentManager
import com.jetbrains.bigdatatools.common.rfs.util.RfsFileUtil
import com.jetbrains.bigdatatools.common.rfs.view.ViewUtil
import com.jetbrains.bigdatatools.common.table.BdiTableProvider
import com.jetbrains.bigdatatools.common.table.editor.BdiTableEditor
import com.jetbrains.bigdatatools.common.table.editor.LoadingLightVirtualFile
import com.jetbrains.bigdatatools.view.messages.BinaryFilesSupportBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class RfsSupportedEditorProvider : WeighedFileEditorProvider() {
  abstract val myFileType: FileType

  override fun accept(project: Project, file: VirtualFile): Boolean =
    file.fileType == myFileType && RfsFileUtil.convertToIOFile(file).exists()

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    val decoratable = BdiDecoratableEditor(LoadingLightVirtualFile(file.name, null), project, file.name) { vFile, prj ->
      val dataframe: DataFrame = withContext(Dispatchers.IO) {
        BdiTableProvider.createDataFrame(vFile)
      }

      withContext(Dispatchers.EDT) {
        BdiTableEditor(vFile, BdiTableProvider.createTable(prj, dataframe))
      }
    }
    val callbacks = ViewUtil.CommonCallbacks(listOf(decoratable), false)

    executeOnPooledThread {
      runBlockingMaybeCancellable {
        val info = try {
          LocalDriverManager.instance.createFileInfo(RfsFileUtil.convertToIOFile(file))
        }
        catch (e: Exception) {
          callbacks.onFail(BinaryFilesSupportBundle.message("bdt.binary.files.cant.load.exception", e.messageOrDefault("")))
          return@runBlockingMaybeCancellable
        }

        val vFile = RfsFileContentManager.getInstance(project).getContentFile(info).result?.let { io ->
          LocalFileSystem.getInstance().refreshAndFindFileByIoFile(io)
        }

        if (!DumbService.isDumb(project) && StartupManager.getInstance(project).postStartupActivityPassed())
          vFile?.refresh(false, false)

        if (vFile != null)
          callbacks.onSuccess(vFile)
        else
          callbacks.onFail(BinaryFilesSupportBundle.message("bdt.binary.files.cant.find.error"))
      }
    }

    return decoratable
  }

  override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}