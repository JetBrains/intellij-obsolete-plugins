package com.intellij.dataWrangler.impl.view.steps

import com.intellij.dataWrangler.impl.view.RoundedCornersJBPanel
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JPanel

class TransformationStepItemComponent(@NlsSafe text: String, index: Int, @NlsSafe description: String, bigBorder: Boolean) : RoundedCornersJBPanel() {
  private val mainBorderInsets = if (bigBorder) UIUtil.PANEL_REGULAR_INSETS else UIUtil.PANEL_SMALL_INSETS

  init {
    border = JBUI.Borders.empty(mainBorderInsets)

    addItemIndex(index)
    val centerPanel = JPanel(VerticalLayout(JBUI.scale(UIUtil.DEFAULT_VGAP)))
    centerPanel.isOpaque = false
    centerPanel.border = JBUI.Borders.emptyLeft(mainBorderInsets.left)
    centerPanel.add(JBLabel("<html><b>${text}</b></html>"))

    addDescription(centerPanel, description)
    add(centerPanel, BorderLayout.CENTER)
  }

  private fun addItemIndex(index: Int) {
    val numberPanel = createIndexNumberPanel(index)
    add(numberPanel, BorderLayout.WEST)
  }

  private fun createIndexNumberPanel(index: Int): JPanel =
    JPanel(BorderLayout()).apply {
      //border = JBUI.Borders.empty(UIUtil.getRegularPanelInsets())
      isOpaque = false
      add(JBLabel(String.format("%02d", index)).apply {
        foreground = JBColor.gray
        font = JBFont.create(Font(Font.MONOSPACED, Font.BOLD, 13))
      }, BorderLayout.NORTH)
    }

  private fun addDescription(centerPanel: JPanel, @NlsSafe description: String) {
    if (description.isNotEmpty()) {
      val descriptionLabel = JBLabel("<html>${description}</html>").apply {
        font = JBFont.small()
        foreground = JBColor.gray
      }
      centerPanel.add(descriptionLabel)
    }
  }
}