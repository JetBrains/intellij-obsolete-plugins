package com.jetbrains.bigdatatools.view.filetypes

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import javax.swing.Icon

class CsvDownloadedFileType : FileType {
  companion object {
    @JvmStatic
    val INSTANCE = CsvDownloadedFileType()
  }

  override fun getName(): String = "Csv(downloaded)"

  @Suppress("HardCodedStringLiteral") // No need to translate file type name.
  override fun getDescription(): String = "Csv(downloaded)"

  override fun getDefaultExtension(): String = "csv(Downloaded)"

  override fun getIcon(): Icon = RfsIcons.CSV_ICON

  override fun isBinary(): Boolean = false
  override fun isReadOnly(): Boolean = true
  override fun getCharset(file: VirtualFile, content: ByteArray): String? = null
}

class TsvDownloadedFileType : FileType {
  companion object {
    @JvmStatic
    val INSTANCE = TsvDownloadedFileType()
  }

  override fun getName(): String = "Tsv(downloaded)"

  @Suppress("HardCodedStringLiteral") // No need to translate file type name.
  override fun getDescription(): String = "Tsv(downloaded)"

  override fun getDefaultExtension(): String = "tsv(Downloaded)"

  override fun getIcon(): Icon = RfsIcons.TSV_ICON

  override fun isBinary(): Boolean = false
  override fun isReadOnly(): Boolean = true
  override fun getCharset(file: VirtualFile, content: ByteArray): String? = null
}

