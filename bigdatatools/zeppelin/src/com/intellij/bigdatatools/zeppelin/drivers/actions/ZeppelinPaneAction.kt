package com.intellij.bigdatatools.zeppelin.drivers.actions

import com.intellij.bigdatatools.zeppelin.rfs.node.ZeppelinRfsTreeNode
import com.intellij.openapi.util.NlsActions
import com.jetbrains.bigdatatools.common.rfs.projectview.actions.RfsPaneOwner
import com.jetbrains.bigdatatools.common.rfs.projectview.actions.RfsProjectPaneActionBase
import com.jetbrains.bigdatatools.common.rfs.projectview.actions.RfsProjectPaneActionContext
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverFileRfsTreeNode
import javax.swing.Icon

abstract class ZeppelinPaneAction : RfsProjectPaneActionBase {
  constructor() : super()
  constructor(@NlsActions.ActionText text: String, icon: Icon? = null) : super(text, icon = icon)

  companion object {
    fun RfsProjectPaneActionContext.isTrash(): Boolean {
      return allSelectedZeppelin(pane) { it.zepRfsPath.isTrash }
    }

    fun RfsProjectPaneActionContext.isNotInTrash(): Boolean {
      return allSelectedZeppelin(pane) { node ->
        !node.zepRfsPath.isInTrash && !node.zepRfsPath.isTrash
      }
    }

    fun RfsProjectPaneActionContext.isInTrash(): Boolean {
      return allSelectedZeppelin(pane) { it.zepRfsPath.isInTrash }
    }

    fun RfsProjectPaneActionContext.isZeppelin(): Boolean {
      return allSelectedZeppelin(pane) { true }
    }

    private fun allSelected(pane: RfsPaneOwner, predicate: (node: DriverFileRfsTreeNode) -> Boolean): Boolean {
      val nodes = pane.getSelectedDriverNodes()
      return nodes.isNotEmpty() && nodes.all(predicate)
    }

    private fun allSelectedZeppelin(pane: RfsPaneOwner, predicate: (node: ZeppelinRfsTreeNode) -> Boolean): Boolean {
      return allSelected(pane) { node ->
        node is ZeppelinRfsTreeNode && predicate(node)
      }
    }
  }
}