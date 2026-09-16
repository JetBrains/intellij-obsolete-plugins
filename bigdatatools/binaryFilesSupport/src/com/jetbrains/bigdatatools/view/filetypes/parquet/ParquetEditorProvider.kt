package com.jetbrains.bigdatatools.view.filetypes.parquet

import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileTypes.FileType
import com.jetbrains.bigdatatools.view.filetypes.RfsSupportedEditorProvider

internal class ParquetEditorProvider : RfsSupportedEditorProvider() {
  override val myFileType: FileType
    get() = ParquetFileType.INSTANCE

  override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_OTHER_EDITORS

  override fun getEditorTypeId(): String = "hdfs-parquet-editor-provider"
}