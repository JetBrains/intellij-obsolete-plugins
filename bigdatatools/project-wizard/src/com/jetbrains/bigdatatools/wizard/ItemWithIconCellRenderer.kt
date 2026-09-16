package com.jetbrains.bigdatatools.wizard

import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.SwingConstants

class ItemWithIconCellRenderer : ListCellRenderer<ItemWithIcon?> {
  override fun getListCellRendererComponent(list: JList<out ItemWithIcon>?,
                                            value: ItemWithIcon?,
                                            index: Int,
                                            isSelected: Boolean,
                                            cellHasFocus: Boolean): Component {
    return JLabel(value?.presentableName, value?.icon, SwingConstants.LEFT)
  }
}