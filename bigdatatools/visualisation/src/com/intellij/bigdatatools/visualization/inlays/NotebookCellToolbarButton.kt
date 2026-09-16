package com.intellij.bigdatatools.visualization.inlays

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.Nls
import java.awt.EventQueue
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel

class NotebookCellToolbarButton(@NlsContexts.Button text: String, icon: Icon) : JPanel(FlowLayout(FlowLayout.LEFT)), MouseListener {

  init {
    border = BorderFactory.createEmptyBorder(0, 20, 0, 20)
    background = null // EditorColorsManager.getInstance().globalScheme.defaultBackground
    isOpaque = false
    add(JLabel(icon))
    add(JLabel(text))
    addMouseListener(this)
  }

  fun setShortcutText(@Nls shortcutText: String) {
    add(JLabel(shortcutText).apply { isEnabled = false })
  }

  protected fun fireActionPerformed(e: ActionEvent?) {
    // Guaranteed to return a non-null array
    val listeners = listenerList.listenerList
    // Process the listeners last to first, notifying
    // those that are interested in this event
    var i = listeners.size - 2
    while (i >= 0) {
      if (listeners[i] === ActionListener::class.java) {
        // Lazily create the event:
        // if (changeEvent == null)
        // changeEvent = new ChangeEvent(this);
        (listeners[i + 1] as ActionListener).actionPerformed(e)
      }
      i -= 2
    }
  }

  override fun mouseClicked(e: MouseEvent?) {
    fireActionPerformed(ActionEvent(this, ActionEvent.ACTION_PERFORMED, null, EventQueue.getMostRecentEventTime(), 0))
  }

  override fun mousePressed(e: MouseEvent?) = Unit

  override fun mouseReleased(e: MouseEvent?) = Unit

  private var mouseHover = false

  override fun mouseEntered(e: MouseEvent?) {
    mouseHover = true
    repaint()
  }

  override fun mouseExited(e: MouseEvent?) {
    mouseHover = false
    repaint()
  }

  fun addActionListener(l: ActionListener) {
    listenerList.add(ActionListener::class.java, l)
  }

  override fun paintComponent(g: Graphics) {
    val g2d = g.create() as Graphics2D
    try {
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2d.color = if (mouseHover) JBUI.CurrentTheme.ActionButton.hoverBackground() else EditorColorsManager.getInstance().globalScheme.defaultBackground
      g2d.fillRoundRect(0, 0, width - 1, height - 1, height, height)
    }
    finally {
      g2d.dispose()
    }
    super.paintComponent(g)
  }
}