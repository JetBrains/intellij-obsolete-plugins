package com.jetbrains.bigdatatools.view.filetypes

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile

abstract class RfsSupportedFileType : FileType {
  // why?
  // Because we need to register decompiler to transform parquet/avro/etc to presentable csv
  override fun isBinary(): Boolean = true
  override fun getCharset(file: VirtualFile, content: ByteArray): String? = "UTF8"
}