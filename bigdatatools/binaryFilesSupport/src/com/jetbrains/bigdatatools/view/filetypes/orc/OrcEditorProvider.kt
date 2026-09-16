package com.jetbrains.bigdatatools.view.filetypes.orc

import com.intellij.openapi.fileTypes.FileType
import com.jetbrains.bigdatatools.view.filetypes.RfsSupportedEditorProvider

internal class OrcEditorProvider : RfsSupportedEditorProvider() {
  companion object {
    private const val PROVIDER_ID = "hdfs-orc-editor-provider"
  }

  override val myFileType: FileType = OrcFileType.INSTANCE

  override fun getEditorTypeId(): String = PROVIDER_ID
}