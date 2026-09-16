package com.intellij.bigdatatools.zeppelin.interpreter

import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.Icon

class ColoredCircleIcon : Icon {

  companion object {
    const val RADIUS = 8
  }

  var color: Color = Color.gray

  private var radius: Int = RADIUS

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2d = g.create() as Graphics2D
    try {
      g2d.color = color
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2d.fillOval(x, y, 8, 8)
    }
    finally {
      g2d.dispose()
    }
  }

  override fun getIconWidth() = radius
  override fun getIconHeight() = radius
}