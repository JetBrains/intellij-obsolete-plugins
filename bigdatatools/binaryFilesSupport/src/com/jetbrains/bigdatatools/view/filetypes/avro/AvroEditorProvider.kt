package com.jetbrains.bigdatatools.view.filetypes.avro

import com.intellij.openapi.fileTypes.FileType
import com.jetbrains.bigdatatools.view.filetypes.RfsSupportedEditorProvider

class AvroEditorProvider : RfsSupportedEditorProvider() {
  companion object {
    private const val PROVIDER_ID = "hdfs-avro-editor-provider"
  }

  override val myFileType: FileType = AvroFileType.INSTANCE

  override fun getEditorTypeId(): String = PROVIDER_ID
}