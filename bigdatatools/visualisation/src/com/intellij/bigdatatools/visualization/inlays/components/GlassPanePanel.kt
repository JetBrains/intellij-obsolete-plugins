package com.intellij.bigdatatools.visualization.inlays.components

import java.awt.Component
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JComponent
import javax.swing.JPanel

class GlassPanePanel(private val editorLastScrollTimestamp: AtomicLong) : JComponent() {

  private var wheelListener: GlassPaneMouseWheelListener? = null

  init {
    isOpaque = false
  }

  override fun contains(x: Int, y: Int): Boolean {
    return System.currentTimeMillis() - editorLastScrollTimestamp.get() < GlassPaneMouseWheelListener.threshold && super.contains(x, y)
  }

  fun addWheelListener(inlayPanel: JPanel, editorContent: Component) {
    if (wheelListener == null)
      wheelListener = GlassPaneMouseWheelListener(inlayPanel, editorContent, editorLastScrollTimestamp)
    addMouseWheelListener(wheelListener)
  }

  fun removeWheelListener() {
    wheelListener?.let { removeMouseWheelListener(it) }
  }
}