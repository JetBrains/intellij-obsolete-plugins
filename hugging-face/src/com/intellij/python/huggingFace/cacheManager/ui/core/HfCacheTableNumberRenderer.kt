package com.intellij.python.huggingFace.cacheManager.ui.core

import javax.swing.SwingConstants

class HfCacheTableNumberRenderer : HfCacheMaterialTableCellRenderer() {
  init {
    horizontalAlignment = SwingConstants.RIGHT
  }
}