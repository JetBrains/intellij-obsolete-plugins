package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.ui.ComponentUtil
import com.intellij.util.ui.MouseEventAdapter
import java.awt.Component
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane

class GlassPaneMouseWheelListener(private val inlayPanel: JPanel,
                                  private val editorContent: Component,
                                  private val editorLastScrollTimestamp: AtomicLong) : MouseWheelListener {
  companion object {

    const val threshold = 300

    fun redispatch(e: MouseEvent, to: JComponent): Boolean {
      val target = ComponentUtil.getParentOfType(JScrollPane::class.java as Class<out JScrollPane?>, to) ?: return false
      MouseEventAdapter.redispatch(e, target)
      return true
    }
  }

  override fun mouseWheelMoved(e: MouseWheelEvent) {
    if ((System.currentTimeMillis() - editorLastScrollTimestamp.get() > threshold && (redispatch(e, inlayPanel)) || e.isShiftDown)) {
      return
    }
    MouseEventAdapter.redispatch(e, editorContent)
  }
}