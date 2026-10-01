package com.intellij.dataWrangler.impl.view.transformation

import com.intellij.dataWrangler.DataWranglerBundle
import com.intellij.dataWrangler.impl.view.transformation.TransformationsNode.TransformationsNodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.setEmptyState
import com.intellij.ui.FilteringTree
import com.intellij.ui.SearchTextField
import com.intellij.util.ui.JBUI
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

internal class TransformationsFilterTree(val project: Project, tree: TransformationsTree, root: TransformationsNode)
  : FilteringTree<TransformationsNode, TransformationsNodeDescriptor>(tree, root) {

  override fun installSearchField(): SearchTextField {
    val searchField = super.installSearchField()

    searchField.textEditor.border = JBUI.Borders.customLine(JBUI.CurrentTheme.Editor.BORDER_COLOR, 1, 0, 1, 0)
    searchField.textEditor.setEmptyState(DataWranglerBundle.message("label.transformations.panel.tree.search.empty.state"))
    searchField.addKeyboardListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent?) {
        if (e?.keyCode == KeyEvent.VK_ENTER) {
          tree.dispatchEvent(e)
        }
      }
    })
    return searchField
  }

  override fun getNodeClass(): Class<out TransformationsNode> = TransformationsNode::class.java
  override fun createNode(obj: TransformationsNodeDescriptor): TransformationsNode = TransformationsNode(obj)
  override fun getChildren(obj: TransformationsNodeDescriptor): Iterable<TransformationsNodeDescriptor?> = obj.children

  /**
   * Criteria to be searched on
   */
  override fun getText(descriptor: TransformationsNodeDescriptor?): String? = when (descriptor) {
    is TransformationsNode.CommandNodeDescriptor<*> -> "${descriptor.command.getGroupName().displayName} ${descriptor.displayName}"
    is TransformationsNode.AIActionNodeDescriptor -> descriptor.commandAction.name
    is TransformationsNode.GroupNodeDescriptor -> descriptor.group.displayName
    else -> null
  }

  override fun onSpeedSearchUpdateComplete(pattern: String?) { // Cell renderer gets number of children dynamically
    tree.repaint()
  }

}