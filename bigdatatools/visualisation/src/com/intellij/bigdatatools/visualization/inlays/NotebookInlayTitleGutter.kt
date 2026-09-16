package com.intellij.bigdatatools.visualization.inlays

import com.intellij.openapi.editor.impl.EditorImpl
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JPanel

class NotebookInlayTitleGutter(private val editor: EditorImpl) : JPanel() {

  var selected = false
    set(value) {
      if (field != value) {
        field = value
        repaint()
      }
    }

  var synced = true
    set(value) {
      if (field != value) {
        field = value
        repaint()
      }
    }

  override fun paintComponent(g: Graphics) {
    val g2d = g.create() as Graphics2D
    try {
      g.color = getCodeCellBackground(editor.colorsScheme)
      g.fillRect(width - NotebookInlayComponent.stripeOffset - 1, 0, NotebookInlayComponent.stripeOffset + 1, height)

      if (selected || !synced) {
        g.color = if (synced) getSelectedCellStripeColor(editor.colorsScheme) else getUnsyncCellStripeColor(editor.colorsScheme)
        g.fillRect(width - NotebookInlayComponent.stripeWidth - NotebookInlayComponent.stripeOffset, 0, NotebookInlayComponent.stripeWidth,
                   height)
      }
    }
    finally {
      g2d.dispose()
    }
  }
}