package com.intellij.bigdatatools.visualization.inlays.components

import java.awt.Graphics
import javax.swing.JPanel

/** Panel that can make their children invisible, but preserving size. */
class ChildrenHidePanel : JPanel(), ToolbarVisibility {

  override var showToolbar = false
    set(value) {
      field = value
      isEnabled = value
    }

  init {
    isOpaque = false
    isEnabled = false
  }

  override fun paintChildren(g: Graphics?) {
    if (showToolbar) {
      super.paintChildren(g)
    }
  }
}