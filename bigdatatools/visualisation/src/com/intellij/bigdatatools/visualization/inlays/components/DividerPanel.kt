package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.bigdatatools.visualization.inlays.utils.NotebookInlayUtils
import com.intellij.ui.scale.JBUIScale
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Panel with custom layout mechanism. The components placed vertically and each component has its own height, also automatically added
 * dividers after each component which can change the height of component above and size of whole panel.
 */
class DividerPanel : JPanel(null) {

  companion object {
    val dividerHeight = JBUIScale.scale(7)
  }

  private val components = mutableListOf<JComponent>()
  private val dividers = mutableListOf<JComponent>()

  val pageComponents: List<JComponent>
    get() = components

  init {
    isOpaque = false
  }

  fun userSizeSet(deltaHeight: Int) {
    firePropertyChange("userSize", 0, deltaHeight)
  }

  fun addPage(component: JComponent) {

    if (components.isNotEmpty()) {
      val divider = OutputDivider(this, components.last())
      dividers.add(divider)
      super.add(divider)
    }

    super.add(component)
    NotebookInlayUtils.doWhenPreferredSizeSet(component, once = true) {
      // Just to emit firePropertyChange("preferredSize", old, preferredSize);
      preferredSize = preferredSize
    }

    components.add(component)
  }

  override fun removeAll() {
    super.removeAll()
    dividers.clear()
    components.clear()
  }

  override fun getMinimumSize(): Dimension {
    val minWidth = components.maxByOrNull { it.minimumSize.width }?.minimumSize?.width ?: 0
    val minHeight = components.sumOf { it.minimumSize.height } + dividers.size * dividerHeight
    return Dimension(minWidth, minHeight)
  }

  override fun getPreferredSize(): Dimension {
    val preferredHeight = components.sumOf { it.preferredSize.height } + dividers.size * dividerHeight
    return Dimension(super.getPreferredSize().width, preferredHeight)
  }

  override fun doLayout() {

    var desiredHeight = dividers.size * dividerHeight
    components.forEach { desiredHeight += if (it.isPreferredSizeSet) it.preferredSize.height else it.height }
    val scale = height.toDouble() / desiredHeight

    var lastY = 0

    for (i in 0 until components.size - 1) {

      val component = components[i]
      val divider = dividers[i]

      var pageHeight = if (component.isPreferredSizeSet) component.preferredSize.height else component.height
      if (scale < 1.0) {
        pageHeight = (pageHeight * scale).toInt()
      }

      component.setBounds(0, lastY, width, pageHeight)
      lastY += pageHeight
      divider.let {
        it.setBounds(0, lastY, width, dividerHeight)
        lastY += dividerHeight
      }
    }

    // last component, if exist, will fit the rest height
    components.lastOrNull()?.setBounds(0, lastY, width, height - lastY)
  }
}