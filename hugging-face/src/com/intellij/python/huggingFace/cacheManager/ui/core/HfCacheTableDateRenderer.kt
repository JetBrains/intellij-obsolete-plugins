package com.intellij.python.huggingFace.cacheManager.ui.core

import java.text.SimpleDateFormat

class HfCacheTableDateRenderer(private val neededAddChecking: Boolean = false) : HfCacheMaterialTableCellRenderer() {

  companion object {
    val df: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss") //DateFormat.getDateInstance(DateFormat.FULL)
  }

  override fun setValue(value: Any?) {
    try {
      if (value == null) {
        super.setValue("")
      }
      else if (neededAddChecking && value.toString().toLong() < 0) {
        super.setValue("-")
      }
      else {
        super.setValue(df.format(value))
      }
    }
    catch (_: Exception) {
      super.setValue(value)
    }
  }
}