package com.jetbrains.bigdatatools.common.table.editor

import com.intellij.charts.dataframe.DataFrameKeys
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.WeighedFileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.bigdatatools.common.data.StructuredFilesUtil
import com.jetbrains.bigdatatools.common.editor.BdiDecoratableEditor
import com.jetbrains.bigdatatools.common.rfs.localcache.RfsFileContentManager
import com.jetbrains.bigdatatools.common.table.BdiTableProvider
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BdiTableEditorProvider : WeighedFileEditorProvider(), DumbAware {
  companion object {
    private const val PROVIDER_ID = "bd-ide-table-editor"
  }

  override fun accept(project: Project, file: VirtualFile): Boolean {
    val extension = FileUtilRt.getExtension(file.name)
    if (extension == StructuredFilesUtil.TABLE_EXTENSION || StructuredFilesUtil.isDownloadedExtension(extension)) return true

    if (file.getUserData(DataFrameKeys.DATA_FRAME) != null) return true

    (file as? LoadingLightVirtualFile)?.underlyingFile?.let {
      if (StructuredFilesUtil.isStructuredFile(it.name) ||
          StructuredFilesUtil.isStructuredFile(file.name) ||
          StructuredFilesUtil.isStructuredFileBom(RfsFileContentManager.getInstance(project).getBomLight(it), project)) return true
    }

    return false
  }

  override fun createEditor(project: Project, file: VirtualFile): BdiDecoratableEditor =
    BdiDecoratableEditor(file, project, MessagesBundle.message("rfs.editor.table.tab.title")) { vFile, prj ->
      val dataframe = withContext(Dispatchers.IO) {
        BdiTableProvider.createDataFrame(vFile)
      }

      withContext(Dispatchers.EDT) {
        BdiTableEditor(vFile, BdiTableProvider.createTable(prj, dataframe))
      }
    }

  override fun getEditorTypeId(): String = PROVIDER_ID

  override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_OTHER_EDITORS

  override fun getWeight(): Double = StructuredFilesUtil.DEFAULT_EDITOR_WEIGHT - 1
}