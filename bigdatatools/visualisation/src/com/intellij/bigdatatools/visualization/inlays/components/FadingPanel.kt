package com.intellij.bigdatatools.visualization.inlays.components

import java.awt.AlphaComposite
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JPanel

/** JPanel with public transparency field. Created completely transparent. */
open class FadingPanel : JPanel() {

  /** Completely transparent by default */
  open var transparency = 0.0f

  init {
    isOpaque = false
  }

  override fun paintChildren(g: Graphics) {
    val graphics = g.create()
    try {
      if (graphics is Graphics2D) {
        graphics.composite = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, transparency)
      }
      super.paintChildren(graphics)
    }
    finally {
      graphics.dispose()
    }
  }
}