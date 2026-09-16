package com.jetbrains.bigdatatools.flink.util

import com.jetbrains.bigdatatools.common.table.renderers.MaterialTableCellRenderer
import com.jetbrains.bigdatatools.common.table.renderers.SplitStringRenderer

class SnakeCaseRenderer : MaterialTableCellRenderer() {
  override fun setValue(value: Any?) {
    val snakeString = value?.toString() ?: ""
    val camelCase = snakeString.split('_').joinToString("") { it.replaceFirstChar { oldChar -> oldChar.uppercaseChar() } }
    text = SplitStringRenderer.camelCaseToReadable(camelCase)
  }
}