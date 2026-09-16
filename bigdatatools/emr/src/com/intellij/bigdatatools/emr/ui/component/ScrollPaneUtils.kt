package com.intellij.bigdatatools.emr.ui.component

import com.intellij.ui.components.JBScrollPane
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.Scrollable

object ScrollPaneUtils {
  // copy from [com.intellij.execution.impl.BaseRCSettingsConfigurable]
  fun wrapWithScrollPane(component: JComponent?): JBScrollPane {
    val scrollPane: JBScrollPane = object : JBScrollPane(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER) {
      override fun getMinimumSize(): Dimension {
        val d = super.getMinimumSize()
        val viewport = getViewport()
        if (viewport != null) {
          val view = viewport.view
          if (view is Scrollable) {
            d.width = (view as Scrollable).preferredScrollableViewportSize.width
          }
          if (view != null) {
            d.width = view.minimumSize.width
          }
        }
        //d.height = max(d.height, JBUIScale.scale(400))
        return d
      }
    }
    scrollPane.border = BorderFactory.createEmptyBorder()
    scrollPane.viewportBorder = BorderFactory.createEmptyBorder()
    if (component != null) {
      scrollPane.viewport.view = component
    }
    return scrollPane
  }
}