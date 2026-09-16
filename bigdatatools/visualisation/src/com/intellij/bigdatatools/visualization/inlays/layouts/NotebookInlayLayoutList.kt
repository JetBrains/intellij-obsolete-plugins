package com.intellij.bigdatatools.visualization.inlays.layouts

import com.intellij.bigdatatools.visualization.inlays.NotebookInlayComponentGutter
import com.intellij.bigdatatools.visualization.inlays.components.ChildrenHidePanel
import com.intellij.bigdatatools.visualization.inlays.components.DividerPanel
import com.intellij.bigdatatools.visualization.inlays.components.DividerPanel.Companion.dividerHeight
import com.intellij.bigdatatools.visualization.inlays.components.HoverPopupAction
import com.intellij.bigdatatools.visualization.inlays.components.ToolbarVisibility
import com.intellij.bigdatatools.visualization.inlays.pages.InlayPage
import com.intellij.bigdatatools.visualization.inlays.pages.InlayPageContentType
import com.intellij.bigdatatools.visualization.inlays.pages.InlaySplitPage
import com.intellij.bigdatatools.visualization.inlays.style.InlaysConfig
import com.intellij.bigdatatools.visualization.inlays.style.InlaysToolbarStyle
import com.intellij.bigdatatools.visualization.inlays.style.OutputToolbarPosition
import com.intellij.bigdatatools.visualization.inlays.utils.NotebookInlayUtils
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

class NotebookInlayLayoutList : NotebookInlayLayout() {

  override val component = DividerPanel()

  override val title = ""

  override val contentType = InlayPageContentType.MIXED

  private var toolbarPanels = mutableListOf<ToolbarVisibility>()

  override var showToolbar = false
    set(value) {
      if (field == value) {
        return
      }
      field = value
      toolbarPanels.forEach { it.showToolbar = value }
      _pages.forEach { (it as? ToolbarVisibility)?.showToolbar = value }
    }

  override fun setPages(inlayPages: List<InlayPage>, clear: Boolean) {
    super.setPages(inlayPages, clear)
    inlayPages.forEach {
      if (it is InlaySplitPage) {
        it.showToolbar = showToolbar
        component.addPage(it.component)
      }
      else {
        component.addPage(createPageWithToolbar(it))
      }
    }
  }

  override fun clear() {
    super.clear()
    toolbarPanels.clear()
  }

  override fun updateGutter(gutter: NotebookInlayComponentGutter) {
    gutter.setToolbarsNumber(component.pageComponents)
    component.pageComponents.forEachIndexed { i, targetComponent ->
      gutter.setToolbarHeight(i, targetComponent.height + dividerHeight)
    }
    gutter.toolbarsPanel.revalidate()
    gutter.toolbarsPanel.repaint()
  }

  override fun dispose() = Unit

  private fun createPageWithToolbar(page: InlayPage): JComponent {
    val actions = page.createActions()
    return if (actions.isNotEmpty()) {

      val toolbarHorizontal = InlaysConfig.getInstance().outputToolbarPosition != OutputToolbarPosition.LEFT &&
                              InlaysConfig.getInstance().outputToolbarPosition != OutputToolbarPosition.RIGHT

      val toolbarPanel = ChildrenHidePanel()

      val toolbar = when (InlaysConfig.getInstance().outputToolbarStyle) {
        InlaysToolbarStyle.FULL -> {
          ToolbarUtils.createActionToolbar(page.component, "BDTNotebookInlayList", actions, toolbarHorizontal).component
        }
        else -> {
          val hoverAction = HoverPopupAction(null, null, AllIcons.Actions.More, actions)
          ToolbarUtils.createActionToolbar(page.component, "BDTNotebookInlayList", DefaultActionGroup(hoverAction), toolbarHorizontal).apply {
            layoutStrategy = ToolbarLayoutStrategy.NOWRAP_STRATEGY
          }.component
        }
      }
      toolbar.isOpaque = !InlaysConfig.getInstance().transparentOutput

      toolbarPanel.showToolbar = showToolbar
      toolbarPanel.add(toolbar)

      toolbarPanels.add(toolbarPanel)

      val panel = JPanel(BorderLayout()).apply {
        isOpaque = !InlaysConfig.getInstance().transparentOutput
        add(page.component, BorderLayout.CENTER)
        add(toolbarPanel, InlaySplitPage.getToolbarPosition())
      }

      NotebookInlayUtils.doWhenPreferredSizeSet(page.component, once = false) {
        panel.preferredSize = Dimension(page.component.preferredSize.width + if (toolbarHorizontal) 0 else toolbar.preferredSize.width,
          page.component.preferredSize.height)
      }

      panel
    }
    else {
      page.component
    }
  }
}