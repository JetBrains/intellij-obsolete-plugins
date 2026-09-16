package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.bigdatatools.visualization.inlays.NotebookInlayComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Weighted
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.IdeGlassPane
import com.intellij.openapi.wm.IdeGlassPaneUtil
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.AWTEvent
import java.awt.BasicStroke
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagLayout
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.abs
import kotlin.math.max

// Partial OnePixelDivider copy to make it possible to resize inlays.
class InlayDivider(private val component: NotebookInlayComponent, vertical: Boolean) : JPanel(GridBagLayout()) {

  private var vertical = false
  private var glassPane: IdeGlassPane? = null
  private val listener = DividerMouseAdapter()
  private var disposable: Disposable? = null
  private var lastMousePoint: Point? = null
  private var dragging = false

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
    enableEvents(AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK)
    setOrientation(vertical)
  }

  override fun paintComponent(g: Graphics) {
    if (!mouseOver && !dragging) {
      return
    }

    val g2d = g.create() as Graphics2D
    try {
      g2d.color = JBUI.CurrentTheme.Editor.BORDER_COLOR
      g2d.stroke = BasicStroke(2f)
      g2d.drawLine(0, height - 2, width, height - 2)
    }
    finally {
      g2d.dispose()
    }
  }

  override fun addNotify() {
    super.addNotify()
    init()
  }

  override fun removeNotify() {
    super.removeNotify()
    if (disposable != null && !Disposer.isDisposed(disposable!!)) {
      Disposer.dispose(disposable!!)
    }
  }

  private fun setDragging(dragging: Boolean) {
    if (this.dragging != dragging) {
      this.dragging = dragging
      if (!dragging && component.userSized) {
        component.onChange?.invoke()
      }
    }
  }

  inner class DividerMouseAdapter : MouseAdapter(), Weighted {
    private fun skipEventProcessing(): Boolean {
      if (isShowing) {
        return false
      }
      setDragging(false)
      glassPane?.setCursor(null, this)
      mouseOver = false
      return true
    }

    override fun mousePressed(e: MouseEvent) {
      if (skipEventProcessing() || e.clickCount > 1) {
        return
      }
      setDragging(isInDragZone(e))
      _processMouseEvent(e)
      if (dragging) {
        e.consume()
      }
    }

    private fun isInDragZone(e: MouseEvent): Boolean {
      val event: MouseEvent = getTargetEvent(e) ?: return false
      val p = event.point
      val vertical = isVertical()
      val d = this@InlayDivider
      if ((if (vertical) p.x else p.y) < 0 || vertical && p.x > d.width || !vertical && p.y > d.height) return false
      val r = abs(if (vertical) p.y else p.x)
      return r < JBUIScale.scale(Registry.intValue("ide.splitter.mouseZone"))
    }

    override fun mouseReleased(e: MouseEvent) {
      if (skipEventProcessing()) {
        return
      }
      _processMouseEvent(e)
      if (dragging) {
        e.consume()
      }
      setDragging(false)
    }

    override fun mouseMoved(e: MouseEvent) {
      if (skipEventProcessing() || getTargetEvent(e) == null) {
        return
      }
      val divider = this@InlayDivider
      if (isInDragZone(e)) {
        glassPane?.setCursor(divider.cursor, divider)
        mouseOver = true
      }
      else {
        glassPane?.setCursor(null, divider)
        mouseOver = false
      }
      _processMouseMotionEvent(e)
    }

    override fun mouseDragged(e: MouseEvent) {
      if (skipEventProcessing()) {
        return
      }
      _processMouseMotionEvent(e)
    }

    override fun getWeight(): Double {
      return 1.0
    }

    private fun _processMouseMotionEvent(e: MouseEvent) {
      val event = getTargetEvent(e)
      if (event == null) {
        glassPane?.setCursor(null, listener)
        mouseOver = false
        return
      }
      processMouseMotionEvent(event)
      if (event.isConsumed) {
        e.consume()
      }
    }

    private fun _processMouseEvent(e: MouseEvent) {
      val event = getTargetEvent(e)
      if (event == null) {
        glassPane?.setCursor(null, listener)
        mouseOver = false
        return
      }
      processMouseEvent(event)
      if (event.isConsumed) {
        e.consume()
      }
    }
  }

  private fun getTargetEvent(e: MouseEvent): MouseEvent? {
    val eventComponent = e.component ?: return null
    val deepestComponentAt = UIUtil.getDeepestComponentAt(eventComponent, e.x, e.y)
    return if (deepestComponentAt == null || !SwingUtilities.isDescendingFrom(deepestComponentAt, getParent())) {
      null //Event is related to some top layer (for example Undock tool window) and we shouldn't process it here
    }
    else SwingUtilities.convertMouseEvent(eventComponent, e, this)
  }

  private fun init() {
    glassPane = IdeGlassPaneUtil.find(this)
    disposable = Disposer.newDisposable()
    glassPane!!.addMouseMotionPreprocessor(listener, disposable!!)
    glassPane!!.addMousePreprocessor(listener, disposable!!)
  }

  private fun setOrientation(vertical: Boolean) {
    this.vertical = vertical
    val cursorType = if (isVertical()) Cursor.N_RESIZE_CURSOR else Cursor.W_RESIZE_CURSOR
    UIUtil.setCursor(this, Cursor.getPredefinedCursor(cursorType))
  }

  override fun processMouseMotionEvent(e: MouseEvent) {
    super.processMouseMotionEvent(e)
    if (MouseEvent.MOUSE_DRAGGED == e.id && dragging) {
      val lastMousePoint = lastMousePoint
      if (isVertical()) {
        if (height > 0 && lastMousePoint != null) {
          val delta = e.locationOnScreen.y - lastMousePoint.y
          val borderInsets = component.border.getBorderInsets(component)
          val height = max(component.minimumSize.height + borderInsets.top + borderInsets.bottom, component.size.height + delta)
          component.setSize(component.size.width, height)
          component.userSized = true
        }
      }
      else {
        if (width > 0 && lastMousePoint != null) {
          val delta = e.locationOnScreen.x - lastMousePoint.x
          val borderInsets = component.border.getBorderInsets(component)
          val width = max(component.minimumSize.width + borderInsets.left + borderInsets.right, component.size.width + delta)
          component.setSize(width, component.size.height)
          component.userSized = true
        }
      }
      this.lastMousePoint = e.locationOnScreen
      e.consume()
    }
  }

  override fun processMouseEvent(e: MouseEvent) {
    super.processMouseEvent(e)
    when (e.id) {
      MouseEvent.MOUSE_CLICKED -> {
        if (e.clickCount > 1) {
          component.restoreSize()
        }
      }
      MouseEvent.MOUSE_PRESSED -> lastMousePoint = e.locationOnScreen
      MouseEvent.MOUSE_RELEASED -> lastMousePoint = null
      MouseEvent.MOUSE_EXITED -> mouseOver = false
    }
  }

  fun isVertical(): Boolean {
    return vertical
  }
}