package com.intellij.python.huggingFace.cacheManager.ui.core

import java.math.RoundingMode
import javax.swing.SwingConstants

class HfCacheTableFileSizeRenderer : HfCacheMaterialTableCellRenderer() {
  init {
    horizontalAlignment = SwingConstants.RIGHT
  }

  override fun setValue(value: Any?) {
    if (value !is Double) super.setValue("-")
    val fileSizeInGb = ((value as Double) / (1000_000_000)).toBigDecimal().setScale(3, RoundingMode.HALF_EVEN)
    super.setValue(fileSizeInGb)
  }
}
