package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.util.ui.TimerUtil
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Installs on given component
 * HoverPopup.installOn(button, actions)
 *
 * Shows popup with provided actions on mouse hover.
 * Hides popup automatically, when mouse is moved outside popup and component area.
 */
class HoverPopup private constructor(private val component: JComponent, private val actions: List<AnAction>) : MouseAdapter() {

  companion object {
    fun installOn(component: JComponent, actions: List<AnAction>) {
      val hoverPopup = HoverPopup(component, actions)
      component.addMouseListener(hoverPopup)
    }

    const val HIDE_DELAY = 500
  }

  private var popup: JBPopup? = null

  private var globalEventListener: AWTEventListener? = null

  private var hideTimer: Timer? = null

  override fun mouseEntered(e: MouseEvent) {
    val popup = popup
    if (popup != null && !popup.isDisposed && popup.isVisible) {
      return
    }
    showPopup(e)

    if (globalEventListener == null) {
      createGlobalEventListener()
    }

    Toolkit.getDefaultToolkit().addAWTEventListener(globalEventListener, AWTEvent.MOUSE_MOTION_EVENT_MASK)
  }

  private fun createGlobalEventListener() {
    globalEventListener = object : AWTEventListener {

      override fun eventDispatched(event: AWTEvent) {

        if (popup == null || popup!!.isDisposed || event.id != MouseEvent.MOUSE_MOVED || event !is MouseEvent) return

        val p1 = Point(event.xOnScreen, event.yOnScreen)
        SwingUtilities.convertPointFromScreen(p1, component)

        val p2 = Point(event.xOnScreen, event.yOnScreen)
        SwingUtilities.convertPointFromScreen(p2, popup!!.content)

        fun Dimension.contains(p: Point) = p.x in 0..width && p.y in 0..height

        if (!component.size.contains(p1) && !popup!!.content.size.contains(p2)) {
          scheduleHide()
        }
        else {
          hideTimer?.stop()
        }
      }
    }
  }

  private fun showPopup(e: MouseEvent) {

    hideTimer?.stop()

    val owner: Component = e.component

    var popup = popup
    if (popup == null || popup.isDisposed) {
      popup = JBPopupFactory.getInstance().createActionGroupPopup(null, DefaultActionGroup(actions),
                                                                  DataManager.getInstance().getDataContext(component),
                                                                  JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false)
      this.popup = popup
    }

    popup.showUnderneathOf(owner)
  }

  private fun scheduleHide() {
    if (hideTimer == null) {
      hideTimer = TimerUtil.createNamedTimer("Hide timer", HIDE_DELAY).apply {
        isRepeats = false
        addActionListener { hidePopup() }
      }
    }

    hideTimer!!.restart()
  }

  private fun hidePopup() {
    if (popup != null && popup!!.isVisible) {
      popup!!.cancel()
      popup = null
    }

    hideTimer?.stop()

    globalEventListener?.let { Toolkit.getDefaultToolkit().removeAWTEventListener(it) }
  }
}
