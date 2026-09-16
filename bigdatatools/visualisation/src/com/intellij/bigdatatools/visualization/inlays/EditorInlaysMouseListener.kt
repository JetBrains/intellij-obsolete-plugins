package com.intellij.bigdatatools.visualization.inlays

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import java.awt.AWTEvent
import java.awt.Point
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

internal interface InlaysMouseListener {
  fun mouseMoved(inlay: NotebookInlayComponent, p: Point)
  fun mouseExited(inlay: NotebookInlayComponent, p: Point)
  fun mouseEntered(inlay: NotebookInlayComponent, p: Point)
}

internal class EditorInlaysMouseListener(private val inlaysManager: EditorInlaysManager, private val editor: Editor) : Disposable {
  private var globalEventListener: AWTEventListener

  private var mouseOverEditor = false

  private var inlayUnderMouse: NotebookInlayComponent? = null

  private val listeners = mutableListOf<InlaysMouseListener>()

  init {
    globalEventListener = object : AWTEventListener {

      override fun eventDispatched(event: AWTEvent) {

        if (!editor.contentComponent.isVisible) return

        if (event.id != MouseEvent.MOUSE_MOVED || event !is MouseEvent) return

        val componentPoint = Point(event.xOnScreen, event.yOnScreen)
        SwingUtilities.convertPointFromScreen(componentPoint, editor.component)

        if (!editor.component.bounds.contains(componentPoint)) {
          if (mouseOverEditor) {
            mouseOverEditor = false

            val point = Point(event.xOnScreen, event.yOnScreen)
            SwingUtilities.convertPointFromScreen(point, editor.contentComponent)
            mouseExited(point)
          }
          return
        }

        val point = Point(event.xOnScreen, event.yOnScreen)
        SwingUtilities.convertPointFromScreen(point, editor.contentComponent)

        mouseOverEditor = true
        mouseMoved(point)
      }
    }

    Toolkit.getDefaultToolkit().addAWTEventListener(globalEventListener, AWTEvent.MOUSE_MOTION_EVENT_MASK)
  }

  fun addListener(listener: InlaysMouseListener) {
    listeners.add(listener)
  }

  fun removeListener(listener: InlaysMouseListener) {
    listeners.remove(listener)
  }

  private fun mouseExited(p: Point) {
    val inlayUnderMouse = inlayUnderMouse
    if (inlayUnderMouse != null) {
      listeners.forEach { it.mouseExited(inlayUnderMouse, p) }
    }
    this.inlayUnderMouse = null
  }

  private fun getInlayUnderMouse(point: Point): NotebookInlayComponent? {
    val position = editor.xyToLogicalPosition(point)
    val offset = editor.logicalPositionToOffset(position)

    return inlaysManager.inlays.values.find {
      val bounds = it.bounds
      (bounds.y..bounds.y + bounds.height).contains(point.y) || it.cellTextRange.containsOffset(offset)
    }
  }

  fun mouseMoved(p: Point) {
    val inlay = getInlayUnderMouse(p)
    val inlayUnderMouse = inlayUnderMouse

    if (inlayUnderMouse == inlay && inlay != null) {
      listeners.forEach { it.mouseMoved(inlay, p) }
      return
    }

    if (inlayUnderMouse != null) {
      listeners.forEach { it.mouseExited(inlayUnderMouse, p) }
    }

    if (inlay != null) {
      listeners.forEach { it.mouseEntered(inlay, p) }
    }

    this.inlayUnderMouse = inlay
  }

  override fun dispose() {
    Toolkit.getDefaultToolkit().removeAWTEventListener(globalEventListener)
  }
}