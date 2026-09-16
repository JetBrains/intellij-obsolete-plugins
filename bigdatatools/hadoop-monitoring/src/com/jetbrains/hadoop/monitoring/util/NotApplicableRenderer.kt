package com.jetbrains.hadoop.monitoring.util

import com.jetbrains.bigdatatools.common.table.renderers.MaterialTableCellRenderer

class NotApplicableRenderer : MaterialTableCellRenderer() {
  override fun setValue(value: Any?) {
    text = if (value?.toString() == "-1") "N/A" else value?.toString() ?: ""
  }
}