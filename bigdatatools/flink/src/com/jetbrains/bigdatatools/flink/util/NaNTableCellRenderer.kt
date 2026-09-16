package com.jetbrains.bigdatatools.flink.util

import com.jetbrains.bigdatatools.common.table.renderers.MaterialTableCellRenderer

class NaNTableCellRenderer : MaterialTableCellRenderer() {
  override fun setValue(value: Any?) {
    text = if (value?.toString() == "NaN") "-" else value?.toString() ?: ""
  }
}