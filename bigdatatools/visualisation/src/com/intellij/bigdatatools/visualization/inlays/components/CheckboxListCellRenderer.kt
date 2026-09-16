package com.intellij.bigdatatools.visualization.inlays.components

import java.awt.Component
import javax.swing.JCheckBox
import javax.swing.JList
import javax.swing.ListCellRenderer

class CheckboxListCellRenderer : JCheckBox(), ListCellRenderer<ColumnsVisibilityDialog.ColumnVisibility> {

  init {
    isFocusPainted = false
    isBorderPainted = true
  }

  override fun getListCellRendererComponent(list: JList<out ColumnsVisibilityDialog.ColumnVisibility>,
                                            value: ColumnsVisibilityDialog.ColumnVisibility?,
                                            index: Int,
                                            isSelected: Boolean,
                                            cellHasFocus: Boolean): Component {
    componentOrientation = list.componentOrientation
    font = list.font
    if (isSelected) {
      background = list.selectionBackground
      foreground = list.selectionForeground
    }
    else {
      background = list.background
      foreground = list.foreground
    }
    setSelected(value?.visible ?: false)

    @Suppress("HardCodedStringLiteral")
    text = value?.name ?: ""
    return this
  }
}