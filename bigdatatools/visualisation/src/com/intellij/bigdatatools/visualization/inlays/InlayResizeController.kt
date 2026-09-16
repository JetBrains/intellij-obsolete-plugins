package com.intellij.bigdatatools.visualization.inlays

import com.intellij.bigdatatools.visualization.inlays.components.InlayDivider
import java.awt.Dimension

/** Resize of InlayComponent with border mouse drag. */
open class InlayResizeController(protected val component: NotebookInlayComponent) {

  private var divider: InlayDivider? = null

  fun uninstall() {
    val divider = divider ?: return
    component.bottom.remove(divider)
    this.divider = null
  }

  fun install() {
    if (divider != null) {
      return
    }

    val divider = InlayDivider(component, true)
    divider.minimumSize = Dimension(divider.minimumSize.width, 4)
    divider.preferredSize = Dimension(divider.preferredSize.width, 4)
    component.bottom.add(divider)
    this.divider = divider
  }
}