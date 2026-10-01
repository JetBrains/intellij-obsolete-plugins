package com.intellij.dataWrangler.impl.view.transformation

import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.hover.TreeHoverListener
import com.intellij.ui.tree.ui.Control
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Cursor
import javax.swing.JTree
import javax.swing.tree.TreeNode
import javax.swing.tree.TreeSelectionModel

internal class TransformationsTree : Tree(), UiDataProvider {

  init {
    putClientProperty(Control.Painter.KEY, Control.Painter.LEAF_WITHOUT_INDENT)
    border = JBUI.Borders.empty(UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP)
    isRootVisible = false
    selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    showsRootHandles = true
    val renderer = TransformationCellRenderer()
    cellRenderer = renderer
    TreeHoverListener.DEFAULT.addTo(this)
    UIUtil.setCursor(this, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
    updateUI()
  }

  override fun getToggleClickCount(): Int = CLICK_TO_EXPAND_COUNT

  private class TransformationCellRenderer : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
      val node = value as? TransformationsNode ?: return
      val item = value.userObject ?: return
      border = if (shouldDrawSeparator(node))
        JBUI.Borders.compound(
          JBUI.Borders.customLineTop(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
        )
      else
        null
      append(item.displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES, true)

      if (!node.isLeaf) {
        append(" ${node.childCount}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
      }
      icon = item.icon ?: EmptyIcon.ICON_16.takeIf { node.isLeaf }

      if (item is TransformationsNode.CommandNodeDescriptor<*>) {
        val description = item.command.getDescription() ?: return
        toolTipText = HtmlBuilder()
          .append(
            HtmlChunk.div()
              .style("width: 200px; word-wrap: break-word;")
              .addText(description) // escaped text
          )
          .toString()
      }

    }

    @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions", "KotlinUnreachableCode")
    private fun shouldDrawSeparator(node: TransformationsNode): Boolean =
      false && isFirstActionNode(node)

    private fun isFirstActionNode(node: TreeNode): Boolean {
      if (!isActionNode(node)) {
        return false
      }
      val idx = node.parent.getIndex(node)
      return idx > 0 && !isActionNode(node.parent?.getChildAt(idx - 1))
    }

    private fun isActionNode(node: TreeNode?): Boolean = node is TransformationsNode && node.userObject is TransformationsNode.AIActionNodeDescriptor
  }

  override fun uiDataSnapshot(sink: DataSink) {
    sink[PlatformDataKeys.SELECTED_ITEM] = selectionPath?.lastPathComponent
  }

  companion object {
    const val CLICK_TO_SWIPE_COUNT = 1
    const val CLICK_TO_EXPAND_COUNT = 1
  }

}