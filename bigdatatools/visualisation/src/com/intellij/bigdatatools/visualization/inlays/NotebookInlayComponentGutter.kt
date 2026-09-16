package com.intellij.bigdatatools.visualization.inlays

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.ExperimentalUI
import com.intellij.ui.scale.JBUIScale
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.CompoundBorder
import kotlin.math.max

/**
 * Special component, which holds and positioned inside NotebookInlayComponent.
 * Displays collapse/expand buttons on gutter edge (depending on current inlay state)
 */
class NotebookInlayComponentGutter(inlayComponent: NotebookInlayComponent) : JPanel(BorderLayout()) {
  private val mainToolbar: ActionToolbar
  val toolbarsPanel = JPanel(VerticalFlowLayout(0, 0))
  private val toolbarList = mutableListOf<Pair<ActionToolbar, JPanel>>()

  private val toolbars: List<Pair<ActionToolbar, JPanel>>
    get() = toolbarList

  private val stripeOffset
    get() = if (ExperimentalUI.isNewUI()) JBUIScale.scale(2) else JBUIScale.scale(8)

  init {
    border = CompoundBorder(EditorInlaysManager.gutterBorder,
                            BorderFactory.createEmptyBorder(InlayDimensions.topBorderUnscaled, 0, InlayDimensions.bottomBorderUnscaled,
                                                            stripeOffset))
    isOpaque = false

    val actionManager = ActionManager.getInstance()
    val actions = actionManager.getAction("BDI.Notebook.Gutter.Main.group") as ActionGroup
    mainToolbar = actionManager.createActionToolbar("BDTInlayGutter", actions, false)
    configureToolbar(mainToolbar)
    mainToolbar.targetComponent = inlayComponent

    toolbarsPanel.isOpaque = false
    toolbarsPanel.add(mainToolbar.component)
    add(toolbarsPanel, BorderLayout.EAST)
  }

  private fun configureToolbar(toolbar: ActionToolbar) {
    val border = toolbar.component.insets
    toolbar.component.border = BorderFactory.createEmptyBorder(0, border.left, border.bottom, border.right)
    toolbar.component.isOpaque = false
    toolbar.layoutStrategy = ToolbarLayoutStrategy.NOWRAP_STRATEGY
  }

  fun setToolbarsNumber(targetComponents: List<JComponent>) {
    val manager = ActionManager.getInstance()
    val count = targetComponents.size
    for (i in toolbarList.size until count) {
      val actions = manager.getAction("BDI.Notebook.Gutter.Inlay.group") as ActionGroup
      val toolbar = manager.createActionToolbar("BDTInlayGutter", actions, false)
      toolbar.targetComponent = targetComponents[i]
      configureToolbar(toolbar)
      val panel = JPanel(BorderLayout()).apply {
        isOpaque = false
        add(toolbar.component, BorderLayout.CENTER) // toolbar ignores preferred size, so we need to wrap it in a panel
      }
      toolbarsPanel.add(panel)
      toolbarList.add(Pair(toolbar, panel))
    }
    for (i in toolbarList.size - 1 downTo count) {
      val (_, panel) = toolbarList.removeAt(i)
      toolbarsPanel.remove(panel)
    }
  }

  // make sure that collapsed gutter isn't too small
  // this method is called from NotebookInlayComponent.getPreferredHeight
  override fun getMinimumSize(): Dimension {
    if (mainToolbar.component.componentCount < 1 || !mainToolbar.component.getComponent(0).isVisible) return super.getMinimumSize()
    val minimumSize = super.getMinimumSize()
    val toolbarInsets = mainToolbar.component.insets
    val iconHeight = mainToolbar.component.getComponent(0).preferredSize.height
    return Dimension(minimumSize.width, iconHeight + toolbarInsets.top + toolbarInsets.bottom)
  }

  fun setToolbarHeight(i: Int, height: Int) {
    val h = if (i == 0) height - mainToolbar.component.preferredSize.height
    else height
    toolbars[i].second.preferredSize = Dimension(-1, max(h, 0))
  }
}