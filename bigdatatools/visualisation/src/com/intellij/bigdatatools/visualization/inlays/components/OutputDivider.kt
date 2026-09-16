package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.openapi.ui.Splitter
import com.intellij.util.ui.JBUI
import java.awt.BasicStroke
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.JPanel
import kotlin.math.max

/** Horizontal divider for DividerPanel which synchronously resizes its component, parent DividerPanel size and calls relayouting  */
class OutputDivider(private val panel: DividerPanel, private val component: Component) : JPanel() {

  private var lastMousePoint: Point? = null

  private var mouseOver = false
    set(value) {
      if (field == value) {
        return
      }

      field = value
      repaint()
    }

  init {
    isFocusable = false
    enableEvents(MouseEvent.MOUSE_EVENT_MASK or MouseEvent.MOUSE_MOTION_EVENT_MASK)
    cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)
    isOpaque = true
  }

  override fun paintComponent(g: Graphics) {
    if (!mouseOver && lastMousePoint == null) {
      return
    }

    val g2d = g.create() as Graphics2D
    try {
      g2d.color = JBUI.CurrentTheme.EditorTabs.borderColor()
      g2d.stroke = BasicStroke(2f)
      g2d.drawLine(0, height - 2, width, height - 2)
    }
    finally {
      g2d.dispose()
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent) {
    super.processMouseMotionEvent(e)

    if (MouseEvent.MOUSE_DRAGGED == e.id) {
      val pointY = lastMousePoint?.y ?: return

      val delta = e.locationOnScreen.y - pointY
      if (delta == 0) {
        return
      }
      val realDelta: Int

      // InlaySplitPage is Splitter and Splitter has no preferred size, so we need to set sizes of splitter first and second components.
      if (component is Splitter) {
        val height = max(max(
          component.minimumSize.height,
          (component.firstComponent?.size?.height ?: 0) + delta),
                         (component.secondComponent?.size?.height ?: 0) + delta)

        realDelta = height - (component.firstComponent?.size?.height ?: component.secondComponent?.size?.height ?: 0)

        component.firstComponent?.let { it.preferredSize = Dimension(it.preferredSize.width, height) }
        component.secondComponent?.let { it.preferredSize = Dimension(it.preferredSize.width, height) }
      }
      else {
        val height = max(component.minimumSize.height, component.size.height + delta)
        realDelta = height - component.size.height
        component.preferredSize = Dimension(component.preferredSize.width, height)
      }

      if (realDelta != 0) {
        panel.userSizeSet(delta)
      }
      lastMousePoint = e.locationOnScreen
      e.consume()
    }
  }

  override fun processMouseEvent(e: MouseEvent) {
    super.processMouseEvent(e)
    when (e.id) {
      MouseEvent.MOUSE_PRESSED -> lastMousePoint = e.locationOnScreen
      MouseEvent.MOUSE_RELEASED -> lastMousePoint = null
      MouseEvent.MOUSE_ENTERED -> mouseOver = true
      MouseEvent.MOUSE_EXITED -> mouseOver = false
    }
  }
}