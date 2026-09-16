package com.jetbrains.bigdatatools.view.filetypes.avro

import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import com.jetbrains.bigdatatools.view.filetypes.RfsSupportedFileType
import javax.swing.Icon

class AvroFileType : RfsSupportedFileType() {
  companion object {
    @JvmStatic
    val INSTANCE = AvroFileType()
  }

  override fun getName(): String = "Avro"

  @Suppress("HardCodedStringLiteral") // No need to translate file type name.
  override fun getDescription(): String = "Avro"

  override fun getDefaultExtension(): String = "avro"

  override fun getIcon(): Icon = RfsIcons.AVRO_ICON
}