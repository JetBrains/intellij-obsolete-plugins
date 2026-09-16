package com.intellij.bigdatatools.visualization.inlays.layouts

import com.intellij.bigdatatools.visualization.inlays.NotebookInlayComponentGutter
import com.intellij.bigdatatools.visualization.inlays.components.ChildrenHidePanel
import com.intellij.bigdatatools.visualization.inlays.components.ToolbarVisibility
import com.intellij.bigdatatools.visualization.inlays.pages.InlayPage
import com.intellij.bigdatatools.visualization.inlays.pages.InlayPageContentType
import com.intellij.bigdatatools.visualization.inlays.pages.InlaySplitPage
import com.intellij.bigdatatools.visualization.inlays.style.InlaysConfig
import com.intellij.bigdatatools.visualization.inlays.style.OutputToolbarPosition
import com.intellij.bigdatatools.visualization.inlays.utils.NotebookInlayUtils
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

class NotebookInlayLayoutSingle : NotebookInlayLayout() {

  override val component = JPanel(BorderLayout()).apply { isOpaque = false }

  override val title
    get() = _pages.firstOrNull()?.title ?: ""

  override val contentType
    get() = _pages.firstOrNull()?.contentType ?: InlayPageContentType.MIXED

  // toolbarPanel exist for all pages except of InlaySplitPage which have own unique toolbar.
  private var toolbarPanel: ChildrenHidePanel? = null

  override var showToolbar = false
    set(value) {
      if (field == value) {
        return
      }
      field = value
      toolbarPanel?.showToolbar = value
      _pages.forEach { (it as? ToolbarVisibility)?.showToolbar = value }
    }

  private fun updatePreferredSize() {
    component.preferredSize = Dimension(component.preferredSize.width, _pages.first().component.preferredSize.height)
  }

  override fun setPages(inlayPages: List<InlayPage>, clear: Boolean) {
    super.setPages(inlayPages, clear)

    if (inlayPages.isEmpty()) {
      return
    }

    val firstPage = inlayPages.first()

    if (firstPage is InlaySplitPage) {
      component.add(firstPage.component, BorderLayout.CENTER)
      firstPage.showToolbar = showToolbar
      this.toolbarPanel = null
    }
    else {
      val toolbarHorizontal = when (InlaysConfig.getInstance().outputToolbarPosition) {
        OutputToolbarPosition.TOP, OutputToolbarPosition.BOTTOM -> true
        OutputToolbarPosition.LEFT, OutputToolbarPosition.RIGHT -> false
      }

      val toolbarPosition = when (InlaysConfig.getInstance().outputToolbarPosition) {
        OutputToolbarPosition.TOP -> BorderLayout.NORTH
        OutputToolbarPosition.LEFT -> BorderLayout.WEST
        OutputToolbarPosition.RIGHT -> BorderLayout.EAST
        OutputToolbarPosition.BOTTOM -> BorderLayout.SOUTH
      }

      val toolbarPanel = ChildrenHidePanel()
      val toolbar = ToolbarUtils.createActionToolbar(firstPage.component, "BDTNotebookInlaySingle", firstPage.createActions(),
        toolbarHorizontal).component.apply {
        isOpaque = false
      }

      toolbarPanel.showToolbar = showToolbar
      toolbarPanel.add(toolbar)

      component.add(firstPage.component, BorderLayout.CENTER)
      component.add(toolbarPanel, toolbarPosition)

      this.toolbarPanel = toolbarPanel
    }

    NotebookInlayUtils.doWhenPreferredSizeSet(firstPage.component, once = false) { updatePreferredSize() }
  }

  override fun updateGutter(gutter: NotebookInlayComponentGutter) {
    val pageComponent = (component.layout as BorderLayout).getLayoutComponent(BorderLayout.CENTER) as? JComponent ?: return
    gutter.setToolbarsNumber(listOf(pageComponent))
    gutter.toolbarsPanel.revalidate()
    gutter.toolbarsPanel.repaint()
  }
}