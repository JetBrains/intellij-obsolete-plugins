package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.components

import com.intellij.ui.ColoredListCellRenderer
import com.intellij.util.ui.NamedColorUtil
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.SwingConstants

class GoToParagraphCellRender : ColoredListCellRenderer<Any>() {

  private val component = JPanel(BorderLayout())
  private val title = JLabel()
  private val description = JLabel()

  init {
    component.add(title, BorderLayout.WEST)
    description.horizontalAlignment = SwingConstants.RIGHT
    component.add(description, BorderLayout.CENTER)
    component.border = BorderFactory.createEmptyBorder(5, 10, 5, 20)
  }

  override fun getListCellRendererComponent(list: JList<out Any>?,
                                            value: Any?,
                                            index: Int,
                                            isSelected: Boolean,
                                            hasFocus: Boolean): Component {

    val paragraphItem = value as ParagraphItem

    component.background = if (isSelected) UIUtil.getListSelectionBackground(true) else null

    title.text = paragraphItem.title
    title.foreground = if (isSelected) NamedColorUtil.getListSelectionForeground(hasFocus) else UIUtil.getLabelForeground()

    description.text = paragraphItem.description
    description.foreground = if (isSelected) title.foreground else NamedColorUtil.getInactiveTextColor()

    return component
  }

  override fun customizeCellRenderer(list: JList<out Any>, value: Any?, index: Int, selected: Boolean, hasFocus: Boolean) {}
}