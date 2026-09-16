package com.jetbrains.bigdatatools.view.filetypes.orc

import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import com.jetbrains.bigdatatools.view.filetypes.RfsSupportedFileType
import javax.swing.Icon

class OrcFileType : RfsSupportedFileType() {
  companion object {
    @JvmStatic
    val INSTANCE = OrcFileType()
  }

  override fun getName(): String = "Orc"

  @Suppress("HardCodedStringLiteral") // No need to translate file type name.
  override fun getDescription(): String = "Orc"

  override fun getDefaultExtension(): String = "orc"

  override fun getIcon(): Icon = RfsIcons.ORC_ICON
}