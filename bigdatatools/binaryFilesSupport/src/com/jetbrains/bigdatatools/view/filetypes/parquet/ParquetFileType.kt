package com.jetbrains.bigdatatools.view.filetypes.parquet

import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import com.jetbrains.bigdatatools.view.filetypes.RfsSupportedFileType
import javax.swing.Icon

/**
 * User: Dmitry.Naydanov
 * Date: 2019-06-25.
 */
class ParquetFileType private constructor () : RfsSupportedFileType() {
  companion object {
    @JvmStatic
    val INSTANCE = ParquetFileType()
  }
  
  override fun getName(): String = "Parquet"

  @Suppress("HardCodedStringLiteral") // No need to translate file type name.
  override fun getDescription(): String = "Parquet"

  override fun getDefaultExtension(): String = "parquet"

  override fun getIcon(): Icon = RfsIcons.PARQUET_ICON
}