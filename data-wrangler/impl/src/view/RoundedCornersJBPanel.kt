package com.intellij.dataWrangler.impl.view

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.UIUtil
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Graphics
import java.awt.Graphics2D


open class RoundedCornersJBPanel(cornerRadius: Int = 24) : JBPanel<RoundedCornersJBPanel>(BorderLayout()) {
  private var isSelected = false
  private val CORNER_RADIUS = JBUIScale.scale(cornerRadius)

  init {
    isOpaque = true
    background = UIUtil.getListSelectionBackground(false)
  }

  override fun paintComponent(g: Graphics) {
    g.color = background
    val config = GraphicsUtil.setupAAPainting(g)
    g.fillRoundRect(0, 0, width, height, CORNER_RADIUS, CORNER_RADIUS)
    if (isSelected) {
      val g2d = g as Graphics2D
      val thickness = JBUIScale.scale(1)
      g2d.color = JBColor.BLUE
      g2d.stroke = BasicStroke(thickness.toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
      g2d.drawRoundRect(0, 0, width - thickness, height - thickness, CORNER_RADIUS, CORNER_RADIUS)
    }
    config.restore()
  }

  internal fun setSelected(isSelected: Boolean) {
    if (this.isSelected == isSelected) return
    this.isSelected = isSelected
    repaint()
  }

}