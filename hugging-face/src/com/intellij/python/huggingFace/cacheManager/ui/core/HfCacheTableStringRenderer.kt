package com.intellij.python.huggingFace.cacheManager.ui.core

class HfCacheTableStringRenderer : HfCacheMaterialTableCellRenderer() {
  override fun setValue(value: Any?) {
    text = if (value == null || value.toString() == "null") "<null>" else value.toString()
  }
}